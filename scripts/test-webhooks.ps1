Param(
  [string]$WecomUrl = "",
  [string]$DingtalkUrl = "",
  [string]$FeishuUrl = "",
  [string]$DingtalkSecret = "",
  [string]$FeishuSecret = "",
  [string]$FeishuAppId = "",
  [string]$FeishuAppSecret = "",
  [string]$FeishuReceiveId = "",
  [string]$FeishuReceiveIdType = "chat_id",
  [string]$FeishuBaseUrl = "https://open.feishu.cn"
)

$ErrorActionPreference = "Stop"

function Load-LocalEnvFile {
  $root = Resolve-Path (Join-Path $PSScriptRoot "..")
  $path = Join-Path $root ".env.backend.local"
  if (-not (Test-Path $path)) { return 0 }

  $count = 0
  Get-Content $path | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) { return }
    $idx = $line.IndexOf("=")
    if ($idx -le 0) { return }
    $key = $line.Substring(0, $idx).Trim()
    $value = $line.Substring($idx + 1).Trim()
    if ($value.StartsWith('"') -and $value.EndsWith('"')) { $value = $value.Substring(1, $value.Length - 2) }
    if ($value.StartsWith("'") -and $value.EndsWith("'")) { $value = $value.Substring(1, $value.Length - 2) }
    if ([string]::IsNullOrWhiteSpace($key)) { return }
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key))) {
      [Environment]::SetEnvironmentVariable($key, $value)
      $count++
    }
  }
  return $count
}

Load-LocalEnvFile | Out-Null

function Resolve-Value([string]$arg, [string]$envName) {
  if (-not [string]::IsNullOrWhiteSpace($arg)) { return $arg.Trim() }
  $v = [Environment]::GetEnvironmentVariable($envName)
  if ([string]::IsNullOrWhiteSpace($v)) { return "" }
  return $v.Trim()
}

function Mask-Url([string]$url) {
  if ([string]::IsNullOrWhiteSpace($url)) { return "<empty>" }
  $q = $url.IndexOf('?')
  if ($q -ge 0) { return $url.Substring(0, $q) + "?***" }
  return $url
}

function Build-Dingtalk-Sign([string]$secret, [long]$timestamp) {
  $toSign = "$timestamp`n$secret"
  $hmac = New-Object System.Security.Cryptography.HMACSHA256
  $hmac.Key = [Text.Encoding]::UTF8.GetBytes($secret)
  $hash = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($toSign))
  $base64 = [Convert]::ToBase64String($hash)
  return [System.Uri]::EscapeDataString($base64)
}

function Build-Feishu-Sign([string]$secret, [long]$timestamp) {
  $toSign = "$timestamp`n$secret"
  $hmac = New-Object System.Security.Cryptography.HMACSHA256
  $hmac.Key = [Text.Encoding]::UTF8.GetBytes($secret)
  $hash = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($toSign))
  return [Convert]::ToBase64String($hash)
}

function Invoke-Webhook([string]$provider, [string]$url, [hashtable]$body) {
  if ([string]::IsNullOrWhiteSpace($url)) {
    return [pscustomobject]@{ provider=$provider; success=$false; statusCode=0; reason="Webhook URL is empty"; url="<empty>"; response="" }
  }

  try {
    $json = $body | ConvertTo-Json -Depth 8
    $resp = Invoke-WebRequest -Uri $url -Method Post -ContentType "application/json; charset=utf-8" -Body $json -TimeoutSec 20
    $flat = ""
    if ($resp.Content) { $flat = ($resp.Content -replace "\s+", " ") }
    return [pscustomobject]@{
      provider=$provider; success=($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 300); statusCode=[int]$resp.StatusCode;
      reason=$resp.StatusDescription; url=(Mask-Url $url); response=$flat.Substring(0, [Math]::Min(220, $flat.Length))
    }
  } catch {
    $statusCode = 0
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) { $statusCode = [int]$_.Exception.Response.StatusCode }
    return [pscustomobject]@{ provider=$provider; success=$false; statusCode=$statusCode; reason=$_.Exception.Message; url=(Mask-Url $url); response="" }
  }
}

function Invoke-Feishu-App([string]$baseUrl, [string]$appId, [string]$appSecret, [string]$receiveId, [string]$receiveIdType, [string]$title, [string]$bodyText) {
  if ([string]::IsNullOrWhiteSpace($appId) -or [string]::IsNullOrWhiteSpace($appSecret)) {
    return [pscustomobject]@{ provider="FEISHU_APP"; success=$false; statusCode=0; reason="App ID/App Secret is empty"; url="<empty>"; response="" }
  }
  if ([string]::IsNullOrWhiteSpace($receiveId)) {
    return [pscustomobject]@{ provider="FEISHU_APP"; success=$false; statusCode=0; reason="Feishu receiveId is empty"; url="<empty>"; response="" }
  }

  try {
    $tokenUrl = "$baseUrl/open-apis/auth/v3/tenant_access_token/internal"
    $tokenReq = @{ app_id=$appId; app_secret=$appSecret } | ConvertTo-Json
    $tokenResp = Invoke-RestMethod -Uri $tokenUrl -Method Post -ContentType "application/json; charset=utf-8" -Body $tokenReq -TimeoutSec 20
    if ($null -eq $tokenResp -or $tokenResp.code -ne 0 -or [string]::IsNullOrWhiteSpace($tokenResp.tenant_access_token)) {
      return [pscustomobject]@{ provider="FEISHU_APP"; success=$false; statusCode=200; reason="Token request rejected"; url=(Mask-Url $tokenUrl); response=($tokenResp | ConvertTo-Json -Compress) }
    }

    $msgUrl = "$baseUrl/open-apis/im/v1/messages?receive_id_type=$receiveIdType"
    $content = @{ text = "$title`n$bodyText" } | ConvertTo-Json -Compress
    $msgBody = @{ receive_id=$receiveId; msg_type="text"; content=$content } | ConvertTo-Json -Compress
    $headers = @{ Authorization = "Bearer $($tokenResp.tenant_access_token)" }
    $msgResp = Invoke-RestMethod -Uri $msgUrl -Method Post -ContentType "application/json; charset=utf-8" -Headers $headers -Body $msgBody -TimeoutSec 20

    $ok = ($msgResp.code -eq 0)
    return [pscustomobject]@{
      provider="FEISHU_APP"; success=$ok; statusCode=200; reason=($(if($ok){"OK"}else{"Message API rejected"}));
      url=(Mask-Url $msgUrl); response=(($msgResp | ConvertTo-Json -Compress).Substring(0, [Math]::Min(220, ($msgResp | ConvertTo-Json -Compress).Length)))
    }
  } catch {
    return [pscustomobject]@{ provider="FEISHU_APP"; success=$false; statusCode=0; reason=$_.Exception.Message; url=(Mask-Url "$baseUrl/open-apis/"); response="" }
  }
}

$wecomUrl = Resolve-Value $WecomUrl "INTEGRATION_WECOM_WEBHOOK_URL"
$dingtalkUrl = Resolve-Value $DingtalkUrl "INTEGRATION_DINGTALK_WEBHOOK_URL"
$feishuUrl = Resolve-Value $FeishuUrl "INTEGRATION_FEISHU_WEBHOOK_URL"
$dingtalkSecret = Resolve-Value $DingtalkSecret "INTEGRATION_DINGTALK_SECRET"
$feishuSecret = Resolve-Value $FeishuSecret "INTEGRATION_FEISHU_SECRET"

$feishuAppId = Resolve-Value $FeishuAppId "INTEGRATION_FEISHU_APP_ID"
$feishuAppSecret = Resolve-Value $FeishuAppSecret "INTEGRATION_FEISHU_APP_SECRET"
$feishuReceiveId = Resolve-Value $FeishuReceiveId "INTEGRATION_FEISHU_RECEIVE_ID"
$feishuReceiveIdType = Resolve-Value $FeishuReceiveIdType "INTEGRATION_FEISHU_RECEIVE_ID_TYPE"
if ([string]::IsNullOrWhiteSpace($feishuReceiveIdType)) { $feishuReceiveIdType = "chat_id" }
$feishuBaseUrl = Resolve-Value $FeishuBaseUrl "INTEGRATION_FEISHU_BASE_URL"
if ([string]::IsNullOrWhiteSpace($feishuBaseUrl)) { $feishuBaseUrl = "https://open.feishu.cn" }

$now = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$title = "CRM connectivity test"
$bodyText = "time: $now`nsource: scripts/test-webhooks.ps1"

$wecomBody = @{ msgtype="markdown"; markdown=@{ content="$title`n$bodyText" } }

$dtFinalUrl = $dingtalkUrl
if (-not [string]::IsNullOrWhiteSpace($dingtalkUrl) -and -not [string]::IsNullOrWhiteSpace($dingtalkSecret)) {
  $ts = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
  $sign = Build-Dingtalk-Sign $dingtalkSecret $ts
  $join = "?"
  if ($dingtalkUrl.Contains("?")) { $join = "&" }
  $dtFinalUrl = "$dingtalkUrl$join" + "timestamp=$ts&sign=$sign"
}
$dingtalkBody = @{ msgtype="markdown"; markdown=@{ title=$title; text="$title`n$bodyText" } }

$fsTs = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$feishuBody = @{ msg_type="text"; content=@{ text="$title`n$bodyText" } }
if (-not [string]::IsNullOrWhiteSpace($feishuSecret)) {
  $feishuBody["timestamp"] = "$fsTs"
  $feishuBody["sign"] = (Build-Feishu-Sign $feishuSecret $fsTs)
}

$results = @()
$results += Invoke-Webhook "WECOM" $wecomUrl $wecomBody
$results += Invoke-Webhook "DINGTALK" $dtFinalUrl $dingtalkBody
$results += Invoke-Webhook "FEISHU" $feishuUrl $feishuBody
$results += Invoke-Feishu-App $feishuBaseUrl $feishuAppId $feishuAppSecret $feishuReceiveId $feishuReceiveIdType $title $bodyText

Write-Output "=== Connectivity Results ==="
$results | Format-Table provider, success, statusCode, reason, url -AutoSize
