@echo off
echo ========================================
echo 推送到云效 Codeup 仓库
echo ========================================
echo.
echo 仓库地址：https://codeup.aliyun.com/632d70d4b5fc2b014e8f323c/Crm
echo.
echo 请按以下步骤操作：
echo.
echo 1. 获取 HTTPS 克隆密码:
echo    - 访问 https://codeup.aliyun.com/
echo    - 个人设置 -> 安全设置 -> HTTPS 克隆密码
echo    - 创建或重置密码（复制生成的密码）
echo.
echo 2. 用户名格式:
echo    632d70d4b5fc2b014e8f323c\18788826840
echo.
echo 3. 执行推送命令后，输入上述凭据
echo.
pause
echo.
echo 开始推送...
git push -u codeup main
echo.
if %ERRORLEVEL% EQU 0 (
    echo ========================================
    echo 推送成功！
    echo 访问仓库：https://codeup.aliyun.com/632d70d4b5fc2b014e8f323c/Crm
    echo ========================================
) else (
    echo ========================================
    echo 推送失败，请检查凭据是否正确
    echo ========================================
)
pause
