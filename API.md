# 安坐 App 后端接口文档

- 版本：v0.0.1
- 更新日期：2026-10-14
- 基础地址：`http://localhost:8080`
- 数据库：MySQL `anzuo`（表结构见 [db.txt](db.txt)）

---

## 1. 通用说明

### 1.1 统一响应结构

所有接口返回如下 JSON 结构，HTTP 状态码与 `code` 一致：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

| code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数错误 / 业务失败 |
| 401 | 未登录、token 无效或已失效 |
| 403 | 无权限（非管理员、账号被禁用、非资源所有者） |
| 404 | 资源不存在 |
| 413 | 请求体过大（超过 1MB） |
| 429 | 操作过于频繁（登录/自助重置失败次数超限被临时锁定） |
| 500 | 服务器内部错误 |

### 1.1.1 请求体大小限制

所有接口的 JSON 请求体硬上限 **1MB**（按 `Content-Length` 判断，超限直接返回 `413`）。图片文件不经后端上传（客户端直传七牛），因此该限制不影响正常业务。

### 1.2 认证方式

除「注册 / 登录 / 自助重置安全码」三个接口外，其余接口均需在请求头携带 JWT：

```
Authorization: Bearer <token>
```

- token 有效期 **7 天**
- **登出后当前 token 立即失效**；密码或安全码**重置后，所有旧 token 立即失效**，需重新登录

### 1.3 安全码（secret_uuid）说明

- 注册时由后端生成原始 UUID（36 位标准格式）返回给用户，**仅此一次**，请提示用户妥善保存
- 数据库只存 BCrypt 哈希，丢失后无法找回，只能重置：
  - 用户自助重置：`POST /api/auth/reset-secret-uuid`（需手机号 + 密码验证身份）
  - 管理员重置：`POST /api/admin/users/{id}/reset-secret-uuid`
- 重置成功后：原安全码作废、所有设备被强制下线、原 token 全部失效

---

## 2. 认证模块 `/api/auth`

### 2.1 注册

`POST /api/auth/register`（无需认证）

注册成功即自动登录，直接返回 token 和安全码原文。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| phone | string | 是 | 手机号，格式 `1[3-9]` 开头 11 位 |
| password | string | 是 | 6-64 位 |
| nickname | string | 否 | 昵称，最长 50 |
| deviceId | string | 否 | 设备唯一标识，最长 128 |
| deviceName | string | 否 | 设备名称，如「小米14」，最长 100 |

请求示例：

```json
{
  "phone": "13800138000",
  "password": "abc12345",
  "nickname": "小明",
  "deviceId": "a1b2c3",
  "deviceName": "小米14"
}
```

响应 `data`：

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "secretUuid": "e2779600-507b-4ec5-9963-2da7d9b1ee9d",
  "user": {
    "id": 1,
    "phone": "13800138000",
    "nickname": "小明",
    "admin": false
  }
}
```

> `secretUuid` 只在注册响应中出现这一次，APP 端应引导用户保存。

错误：`400 该手机号已注册`、`400 手机号格式不正确`、`400 密码长度需在6-64位之间`

### 2.2 登录

`POST /api/auth/login`（无需认证）

三项凭据全部校验通过才签发 token；任一错误均返回同一提示，不区分具体项。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| phone | string | 是 | 手机号 |
| password | string | 是 | 登录密码明文 |
| secretUuid | string | 是 | 安全码原文 |
| deviceId | string | 否 | 设备唯一标识 |
| deviceName | string | 否 | 设备名称 |

响应 `data`：同注册（`secretUuid` 固定为 `null`）。

每次登录会写入一条设备审计记录（含登录 IP、时间、tokenVersion）。

**失败锁定**：同一手机号 15 分钟内登录失败累计 5 次后暂时锁定，期间所有登录尝试直接返回 `429`（不论凭据是否正确）；登录成功后计数清零。

错误：`400 手机号、密码或安全码错误`、`403 账号已被禁用，请联系管理员`、`429 操作过于频繁，请15分钟后再试`

### 2.3 自助重置安全码

`POST /api/auth/reset-secret-uuid`（无需认证）

用户丢失安全码时，凭 **手机号 + 密码** 验证身份后换取新安全码。不能仅凭手机号重置。

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| phone | string | 是 | 手机号 |
| password | string | 是 | 登录密码明文 |

响应 `data`：

```json
{
  "secretUuid": "8137be3b-1d6f-418b-86cb-3273731dbbfe",
  "notice": "安全码已重置，原安全码作废，所有设备已被强制下线"
}
```

副作用：`token_version + 1`，所有旧 token 立即失效，设备审计中在线会话全部关闭。

**失败锁定**：与登录共用同一策略（同一手机号 15 分钟内失败 5 次锁定，返回 `429`）。

错误：`400 手机号、密码或安全码错误`、`403 账号已被禁用，请联系管理员`、`429 操作过于频繁，请15分钟后再试`

### 2.4 退出登录

`POST /api/auth/logout`（需认证）

关闭当前 token 对应的那条设备会话（写入 `logout_time`），**该 token 立即失效**，再次使用返回 `401`。响应 `data` 为 `null`。

### 2.5 获取当前用户信息

`GET /api/auth/me`（需认证）

响应 `data`：

```json
{ "id": 1, "phone": "13800138000", "nickname": "小明", "admin": false }
```

### 2.6 登录设备列表

`GET /api/auth/devices`（需认证）

按登录时间倒序返回最近 50 条。

> 每个用户仅保留最近 50 条登录记录，新登录会自动清理更早的历史记录（被清理的记录若仍在线，对应 token 会随之失效）。

响应 `data`：

```json
[
  {
    "id": 1,
    "deviceId": "a1b2c3",
    "deviceName": "小米14",
    "loginIp": "120.230.1.2",
    "loginTime": "2026-10-14T22:46:56",
    "logoutTime": null
  }
]
```

`logoutTime` 为 `null` 表示该会话仍在线。

---

## 3. 图片模块 `/api/photo`

上传采用 **七牛客户端直传**，流程：

```
① GET /api/photo/upload-token  → 拿到 key + uploadToken
② 客户端将文件直传七牛（表单上传，见 3.2 附注）
③ POST /api/photo/confirm      → 后端核实文件真实大小、扣减额度、正式入库
```

容量限制（均按**自然月**统计，每月初自动从 0 重新计算）：

- 单用户每月上传上限 **500MB**（`app.upload.user-monthly-limit`）
- **所有用户当月上传合计上限 10GB**（`app.upload.global-limit`），即全体用户同一个月内上传总量不能超过 10GB，次月 1 日自动恢复为 0 重新累计

额度不足时 confirm 会删除已上传的文件并返回错误；全局额度在取上传凭证时也会预检查。

### 3.1 获取直传凭证

`GET /api/photo/upload-token?fileName=test.jpg`（需认证）

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| fileName | string | 否 | 原始文件名，用于提取扩展名（缺省按 jpg 处理） |

响应 `data`：

```json
{
  "key": "photo/1/202610/ed754c3d805c4c65afb315855688e9f6.jpg",
  "uploadToken": "N2oJWR...:eyJpbnNlcnRPbmx5Ijox...",
  "bucket": "yfdljt",
  "domain": "http://img.mornbussion.site",
  "expireSeconds": 3600
}
```

- `key` 由后端生成，直传时必须使用该 key
- 凭证为 **insertOnly**（禁止覆盖已有文件），有效期 1 小时

错误：`400 本月上传额度已用完`、`400 本月服务器总上传额度已用完`

### 3.2 客户端直传七牛（非本服务接口）

凭证拿到后，用 HTTP 表单直传七牛：

```
POST https://up-z2.qiniup.com/        ← 上传域名按 bucket 所属区域选择
Content-Type: multipart/form-data

token=<uploadToken>
key=<key>
file=<图片二进制>
```

常用上传域名：华东 `up-z0.qiniup.com`、华北 `up-z1.qiniup.com`、华南 `up-z2.qiniup.com`、北美 `up-na0.qiniup.com`、东南亚 `up-as0.qiniup.com`。请按七牛控制台中 bucket `yfdljt` 的存储区域选择。

返回 200 即上传成功，随后调用 confirm 确认。

### 3.3 确认上传（入库）

`POST /api/photo/confirm`（需认证）

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| qiniuKey | string | 是 | 直传时使用的 key（必须是**本用户**的 key，格式 `photo/{userId}/...`） |
| photoName | string | 是 | 原始文件名，最长 255 |
| width | int | 否 | 图片宽（0-50000） |
| height | int | 否 | 图片高（0-50000） |

后端行为：校验 key 归属 → 向七牛核实文件存在并取回真实大小 → 原子扣减月度/全局额度 → 写入 `t_photo`。任一步失败会删除七牛上的孤儿文件并回滚。

响应 `data`：

```json
{
  "id": 1,
  "photoName": "test.jpg",
  "fileSize": 204800,
  "width": 1080,
  "height": 1920,
  "uploadTime": "2026-10-14T23:00:00",
  "url": "http://img.mornbussion.site/photo/1/202610/ed75....jpg?e=1791994800&token=..."
}
```

`url` 为私有空间签名链接，**2 小时后过期**，过期后重新调 `GET /{id}/url` 获取。

错误：`403 非法的文件标识`（使用了他人或非法格式的 key）、`400 文件尚未上传成功，请重新上传`、`400 本月上传额度不足，该文件已被清除`、`400 本月服务器总上传额度已用完，该文件已被清除`、`400 文件无效，请重新上传`、`400 宽度不合法 / 高度不合法`

### 3.4 分页查询我的图片

`GET /api/photo/page?page=1&size=20`（需认证）

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| page | int | 否 | 1 | 页码，从 1 开始 |
| size | int | 否 | 20 | 每页条数，最大 100 |

按上传时间倒序。响应 `data`：

```json
{
  "list": [ { "id": 1, "photoName": "test.jpg", "fileSize": 204800, "width": 1080, "height": 1920, "uploadTime": "2026-10-14T23:00:00", "url": "http://..." } ],
  "total": 1,
  "page": 1,
  "size": 20
}
```

### 3.5 获取图片访问链接

`GET /api/photo/{id}/url`（需认证）

仅本人或管理员可访问他人图片。响应 `data` 为签名 URL 字符串（2 小时有效）。

错误：`404 图片不存在`、`403 无权访问该图片`

### 3.6 删除图片

`DELETE /api/photo/{id}`（需认证）

仅本人或管理员可删除。软删除 DB 记录 + 删除七牛物理文件 + 按上传月份回退个人月度统计与全局统计。响应 `data` 为 `null`。

错误：`404 图片不存在`、`403 无权删除该图片`

---

## 4. 管理员模块 `/api/admin`

以下接口仅 `is_admin=1` 的用户可调用，普通用户返回 `403 无权限操作`。

### 4.1 用户列表

`GET /api/admin/users?phone=138&page=1&size=20`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| phone | string | 否 | 手机号模糊搜索 |
| page / size | int | 否 | 默认第 1 页、20 条，最大 100 |

响应 `data`：

```json
{
  "list": [
    { "id": 1, "phone": "13800138000", "nickname": "测试用户", "admin": false, "enable": true, "createTime": "2026-10-14T21:53:38" }
  ],
  "total": 1,
  "page": 1,
  "size": 20
}
```

> 用户响应中永不返回密码哈希与安全码哈希。

### 4.2 管理员重置用户安全码

`POST /api/admin/users/{id}/reset-secret-uuid`

`{id}` 为用户 ID（可从 4.1 列表按手机号查找）。返回新安全码原文，由管理员转发给用户。

响应 `data`：

```json
{
  "secretUuid": "03330506-4637-4b92-8b37-8b3610753221",
  "notice": "安全码已重置，原安全码作废，所有设备已被强制下线"
}
```

副作用与自助重置相同：`token_version + 1`，该用户全部旧 token 失效。

错误：`404 用户不存在`

### 4.3 全局月度存储用量

`GET /api/admin/storage`

返回**当前自然月**所有用户的上传合计用量；月初尚无上传记录时 `totalSize` 为 `0`、`updateTime` 为 `null`。

响应 `data`：

```json
{ "statMonth": "202610", "totalSize": 0, "limit": 10737418240, "updateTime": null }
```

`statMonth` 统计月份（yyyyMM），`totalSize` 当月已用字节，`limit` 配置的全局月度上限（10GB）。

---

## 5. 测试账号

| 手机号 | 密码 | 安全码 | 角色 |
|---|---|---|---|
| 13800138000 | 123456 | 11111111-1111-1111-1111-111111111111 | 普通用户 |
| 13900139000 | 123456 | 22222222-2222-2222-2222-222222222222 | 管理员 |

> 种子数据定义于 [db.txt](db.txt)。若执行过重置，以重置返回的新安全码为准。

---

## 6. 快速联调示例（curl）

```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","password":"123456","secretUuid":"11111111-1111-1111-1111-111111111111"}'

# 获取直传凭证（替换 <token>）
curl "http://localhost:8080/api/photo/upload-token?fileName=demo.jpg" \
  -H "Authorization: Bearer <token>"

# 我的图片分页
curl "http://localhost:8080/api/photo/page?page=1&size=20" \
  -H "Authorization: Bearer <token>"

# 管理员查看用户列表
curl "http://localhost:8080/api/admin/users?page=1&size=20" \
  -H "Authorization: Bearer <admin_token>"
```
