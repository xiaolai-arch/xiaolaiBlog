#!/usr/bin/env python3
"""Run once on the server over SSH. Create and remove isolated acceptance accounts."""
import concurrent.futures
import json
import os
import secrets
import subprocess
import urllib.error
import urllib.request

BASE = 'http://127.0.0.1:8080'
names = ['qa_' + secrets.token_hex(6) for _ in range(2)]
password = secrets.token_hex(16)
checks = []


def sql(statement):
    env = dict(os.environ, MYSQL_PWD=os.environ['DB_PASSWORD'])
    result = subprocess.run(['mysql', '--host=' + os.environ.get('DB_HOST', '127.0.0.1'),
                             '--port=' + os.environ.get('DB_PORT', '3306'), '--user=' + os.environ['DB_USERNAME'],
                             '--batch', '--skip-column-names', os.environ.get('DB_NAME', 'xiaolai_blog')],
                            input=statement, universal_newlines=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=env, check=True)
    return result.stdout.strip()


def api(method, path, payload=None, token=None, expected=200):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = urllib.request.Request(BASE + '/api/v1' + path, method=method, headers=headers,
                                     data=None if payload is None else json.dumps(payload).encode())
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            status, body = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, body = error.code, error.read()
        error.close()
    assert status == expected, '{} {}: HTTP {}'.format(method, path, status)
    if expected != 200:
        return None
    result = json.loads(body)
    assert result['code'] == 200, '{} {}: business code {}'.format(method, path, result['code'])
    return result.get('data')


def login(name, value):
    return api('POST', '/admin/auth/login', {'username': name, 'password': value})['token']


try:
    users = [api('POST', '/auth/register', {'username': name, 'password': password}) for name in names]
    a, b = [user['token'] for user in users]
    api('GET', '/admin/articles', token=b, expected=403)
    note = api('POST', '/notes', {'title': 'Acceptance only', 'content': 'draft A'}, a)
    api('PUT', '/notes/' + str(note['id']), {'content': 'saved A'}, a)
    assert api('GET', '/notes/' + str(note['id']), token=a)['content'] == 'saved A'
    assert all('content' not in item for item in api('GET', '/notes', token=a))
    assert not api('GET', '/notes', token=b)
    # Non-owned notes use the project's business-level 404 response.
    request = urllib.request.Request(BASE + '/api/v1/notes/' + str(note['id']), headers={'Authorization': 'Bearer ' + b})
    with urllib.request.urlopen(request, timeout=10) as response:
        assert json.load(response)['code'] == 404
    checks.append('registration, role isolation, note save/read/list and account isolation')
    def upload(item):
        value = json.dumps({'version': 2, 'items': {item: {'done': True, 'at': 100}}})
        api('PUT', '/progress/leetcode', {'value': value}, a)
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as workers:
        list(workers.map(upload, ['device-a', 'device-b']))
    value = json.dumps({'version': 2, 'items': {'device-a': {'done': False, 'at': 200}}})
    api('PUT', '/progress/leetcode', {'value': value}, a)
    upload('device-a')
    state = json.loads(api('GET', '/progress/leetcode', token=a)['value'])['items']
    assert not state['device-a']['done'] and state['device-b']['done']
    assert api('GET', '/progress/leetcode', token=b)['value'] == ''
    checks.append('simultaneous device progress, cancellation and private progress isolation')
    # Give only this newly created account temporary admin rights to test password changes.
    sql("UPDATE `user` SET role='admin' WHERE username='{}';".format(names[0]))
    new_password = secrets.token_hex(16)
    api('PUT', '/admin/profile/password', {'oldPassword': password, 'newPassword': new_password}, a)
    api('GET', '/notes', token=a, expected=401)
    a = login(names[0], new_password)
    api('GET', '/notes', token=a)
    api('POST', '/admin/auth/logout', {}, a)
    api('GET', '/notes', token=a, expected=401)
    checks.append('password change and logout revoke previous tokens')
    for endpoint in ['/articles?page=1&size=1', '/music/current', '/gallery-frames']:
        api('GET', endpoint)
    checks.append('public article, music and gallery APIs')
finally:
    # Generated names are alphanumeric/underscore only; never touch existing account names.
    quoted = ','.join("'{}'".format(name) for name in names)
    sql('DELETE n FROM note n JOIN `user` u ON u.id=n.user_id WHERE u.username IN (' + quoted + ');'
        'DELETE p FROM user_progress p JOIN `user` u ON u.id=p.user_id WHERE u.username IN (' + quoted + ');'
        'DELETE FROM login_log WHERE username IN (' + quoted + ');'
        'DELETE FROM `user` WHERE username IN (' + quoted + ');')
    assert sql('SELECT COUNT(*) FROM `user` WHERE username IN (' + quoted + ');') == '0'
print(json.dumps({'ok': True, 'checks': checks, 'temporary_accounts_removed': True}, indent=2))
