# 推送到云效 Codeup 仓库指南

## ✅ 已完成配置
- Git 用户名已设置：`18788826840`
- Git 邮箱已设置：`18788826840`
- Remote 已配置：`codeup -> https://codeup.aliyun.com/632d70d4b5fc2b014e8f323c/Crm.git`

## 🔐 获取 HTTPS 克隆凭据

### 步骤 1：登录云效
访问：https://codeup.aliyun.com/

### 步骤 2：进入个人设置
1. 点击右上角头像
2. 选择"个人设置"

### 步骤 3：生成 HTTPS 克隆密码
1. 左侧菜单选择"安全设置"
2. 找到"HTTPS 克隆密码"
3. 点击"创建密码"或"重置密码"
4. 复制生成的密码（只显示一次，请妥善保存）

### 步骤 4：获取用户名
用户名格式：`组织 ID\阿里云账号`
```
用户名：632d70d4b5fc2b014e8f323c\18788826840
密码：(步骤 3 中生成的密码)
```

## 🚀 执行推送

在 PowerShell 中执行以下命令：
```powershell
git push -u codeup main
```

当弹出凭据输入框时：
- **Username**: `632d70d4b5fc2b014e8f323c\18788826840`
- **Password**: (步骤 3 生成的密码)

## ✨ 验证推送成功

推送成功后，访问仓库查看代码：
https://codeup.aliyun.com/632d70d4b5fc2b014e8f323c/Crm

## 📝 常用命令

```powershell
# 查看远程仓库
git remote -v

# 查看分支
git branch -a

# 拉取最新代码
git pull codeup main

# 推送代码
git push codeup main
```

## ⚠️ 注意事项

1. HTTPS 克隆密码只在创建时显示一次，请妥善保存
2. 如果忘记密码，可以重新生成，但需要更新本地凭据
3. 首次推送后，后续操作会自动使用保存的凭据
4. 建议使用 SSH 密钥认证（更安全，无需每次输入密码）
