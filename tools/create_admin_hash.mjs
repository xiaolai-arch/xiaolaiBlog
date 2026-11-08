import { createRequire } from 'node:module'

const password = process.env.BLOG_ADMIN_PASSWORD
if (!password || password.length < 12 || Buffer.byteLength(password, 'utf8') > 72) {
  console.error('Set BLOG_ADMIN_PASSWORD to 12-72 UTF-8 bytes. The value is never printed.')
  process.exit(1)
}

const require = createRequire(new URL('../blog-frontend/package.json', import.meta.url))
const bcrypt = require('bcryptjs')
process.stdout.write(bcrypt.hashSync(password, 12))
