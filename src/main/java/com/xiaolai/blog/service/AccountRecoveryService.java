package com.xiaolai.blog.service;

import com.xiaolai.blog.common.exception.BlogException;
import com.xiaolai.blog.security.LoginRateLimiter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AccountRecoveryService {
    private final JdbcTemplate db;
    private final ObjectProvider<JavaMailSender> mail;
    private final PasswordEncoder passwords;
    private final String baseUrl,from;
    private final LoginRateLimiter limiter=new LoginRateLimiter();
    public AccountRecoveryService(JdbcTemplate db,ObjectProvider<JavaMailSender> mail,PasswordEncoder passwords,
            @Value("${app.public-url:http://localhost:3000}") String baseUrl,@Value("${spring.mail.username:}") String from) {
        this.db=db; this.mail=mail; this.passwords=passwords; this.baseUrl=baseUrl; this.from=from;
    }
    public boolean available() {
        try {
            var url = java.net.URI.create(baseUrl);
            return mail.getIfAvailable() != null && !from.isBlank() && "https".equals(url.getScheme())
                    && url.getHost() != null && url.getUserInfo() == null && url.getQuery() == null && url.getFragment() == null;
        } catch (IllegalArgumentException e) { return false; }
    }
    private void ready(HttpServletRequest request) {
        if(!available()) throw new BlogException(503,"邮件找回暂未开通，请联系站点管理员");
        if(!limiter.tryAcquire(LoginRateLimiter.clientIp(request))) throw new BlogException(429,"请求过于频繁，请稍后重试");
    }
    @Transactional
    public void verifyEmail(long userId,String email,HttpServletRequest request) {
        ready(request);
        String normalized=email.trim().toLowerCase(java.util.Locale.ROOT);
        send(userId,"verify",normalized);
    }
    @Transactional
    public void requestReset(String username,HttpServletRequest request) {
        ready(request);
        var rows=db.queryForList("SELECT e.user_id,e.email FROM account_email e JOIN `user` u ON u.id=e.user_id WHERE u.username=? AND e.verified=1",username);
        if(!rows.isEmpty()) send(((Number)rows.getFirst().get("user_id")).longValue(),"reset",(String)rows.getFirst().get("email"));
    }
    private void send(long userId,String purpose,String email) {
        byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes); String token=HexFormat.of().formatHex(bytes);
        db.update("DELETE FROM account_token WHERE (user_id=? AND purpose=?) OR expires_at<CURRENT_TIMESTAMP",userId,purpose);
        db.update("INSERT INTO account_token(token_hash,user_id,purpose,email,expires_at) VALUES(?,?,?,?,?)",hash(token),userId,purpose,email,LocalDateTime.now().plusMinutes(30));
        var message=new SimpleMailMessage(); message.setFrom(from); message.setTo(email);
        message.setSubject(purpose.equals("verify")?"验证博客邮箱":"重置博客密码");
        message.setText("请在 30 分钟内打开链接：\n"+baseUrl.replaceAll("/$","")+"/login?"+purpose+"="+token+"\n如非本人操作，请忽略此邮件。");
        try { mail.getObject().send(message); }
        catch (org.springframework.mail.MailException e) { throw new BlogException(503,"邮件暂时无法发送，请稍后重试"); }
    }
    @Transactional
    public void confirm(String token,String purpose,String newPassword) {
        if(token==null || !token.matches("[a-f0-9]{64}")) throw new BlogException(422,"链接无效或已过期");
        var rows=db.queryForList("SELECT user_id,email FROM account_token WHERE token_hash=? AND purpose=? AND expires_at>CURRENT_TIMESTAMP FOR UPDATE",hash(token),purpose);
        if(rows.isEmpty()) throw new BlogException(422,"链接无效或已过期");
        long user=((Number)rows.getFirst().get("user_id")).longValue(); String email=(String)rows.getFirst().get("email");
        if(purpose.equals("verify")) {
            db.update("DELETE FROM account_email WHERE user_id=?",user);
            db.update("INSERT INTO account_email(user_id,email,verified) VALUES(?,?,1)",user,email);
            db.update("DELETE FROM account_token WHERE user_id=? AND purpose='reset'",user);
        } else {
            if(db.queryForObject("SELECT COUNT(*) FROM account_email WHERE user_id=? AND email=? AND verified=1",Integer.class,user,email)!=1) throw new BlogException(422,"链接无效或已过期");
            if(newPassword==null || newPassword.length()<8 || newPassword.length()>64 || newPassword.getBytes(StandardCharsets.UTF_8).length>72) throw new BlogException(422,"密码需要 8–64 位且不超过 72 字节");
            db.update("UPDATE `user` SET password=?,token_version=token_version+1,login_attempts=0,lock_time=NULL WHERE id=?",passwords.encode(newPassword),user);
        }
        db.update("DELETE FROM account_token WHERE user_id=? AND purpose=?",user,purpose);
    }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
