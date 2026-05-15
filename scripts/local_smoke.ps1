#requires -version 5
<#
本地 smoke 测试：先确保后端已经在 http://127.0.0.1:8080/api 运行
（在 backend 目录执行：./mvnw.cmd spring-boot:run），然后跑本脚本。
用法：
  pwsh ./scripts/local_smoke.ps1               # 默认全部 3 个场景
  pwsh ./scripts/local_smoke.ps1 -Only consult # 只跑咨询
  pwsh ./scripts/local_smoke.ps1 -Base http://127.0.0.1:8081/api
#>
param(
    [string]$Base = "http://127.0.0.1:8080/api",
    [ValidateSet("all","consult","review","draft")][string]$Only = "all"
)

$ErrorActionPreference = "Stop"

function Show-Cites($obj, [string[]]$fields) {
    $allText = ""
    foreach ($f in $fields) {
        $v = $obj.$f
        if ($null -eq $v) { continue }
        if ($v -is [System.Array]) {
            foreach ($item in $v) {
                if ($item -is [string]) { $allText += " " + $item }
                else {
                    foreach ($p in $item.PSObject.Properties) {
                        if ($p.Value -is [string]) { $allText += " " + $p.Value }
                    }
                }
            }
        } elseif ($v -is [string]) { $allText += " " + $v }
    }
    $matches = [regex]::Matches($allText, '\[[LCK]\d+\]')
    Write-Host ("citations found: {0} -> {1}" -f $matches.Count, ($matches | ForEach-Object { $_.Value } | Select-Object -First 10 -Join ' '))
}

function Hit($name, $path, $body) {
    Write-Host ""
    Write-Host "============================="
    Write-Host $name
    Write-Host "============================="
    $json = $body | ConvertTo-Json -Depth 6 -Compress
    try {
        $resp = Invoke-RestMethod -Method Post -Uri "$Base$path" -Body $json -ContentType "application/json; charset=utf-8" -TimeoutSec 120
    } catch {
        Write-Host "REQUEST FAILED: $($_.Exception.Message)"
        return
    }
    return $resp.data
}

if ($Only -in @("all","consult")) {
    $d = Hit "1. consultation" "/legal/consultation" @{
        question = "公司未签劳动合同且拖欠工资两个月，我应当如何主张权利？"
        facts    = @("已入职6个月","存在微信工作记录")
    }
    if ($d) {
        Write-Host ("category: {0}, confidence: {1}" -f $d.category, $d.confidence)
        Write-Host ("legalBasis: {0}, recommendations: {1}, riskAlerts: {2}" -f $d.legalBasis.Count, $d.recommendations.Count, $d.riskAlerts.Count)
        Show-Cites $d @("legalBasis","recommendations","riskAlerts")
        if ($d.legalBasis.Count -gt 0) { Write-Host ("first legalBasis: " + $d.legalBasis[0].Substring(0,[Math]::Min(220,$d.legalBasis[0].Length))) }
    }
}

if ($Only -in @("all","review")) {
    $d = Hit "2. contract-review" "/legal/contract-review" @{
        contractTitle   = "软件开发服务合同"
        contractContent = "本合同由甲方与乙方签订，乙方为甲方提供定制化软件开发服务。合同期限为6个月，总金额为人民币20万元。乙方应在合同签订后3个月内交付初版系统。本合同最终解释权归甲方所有。"
    }
    if ($d) {
        Write-Host ("confidence: {0}, risks: {1}, missing: {2}" -f $d.confidence, $d.risks.Count, $d.missingClauses.Count)
        if ($d.summary) { Write-Host ("summary: " + $d.summary.Substring(0, [Math]::Min(220, $d.summary.Length))) }
        $textBag = @()
        $textBag += $d.summary
        foreach ($r in $d.risks) { $textBag += $r.issue; $textBag += $r.suggestion }
        $textBag += $d.missingClauses
        $allText = ($textBag | Where-Object { $_ }) -join " "
        $m = [regex]::Matches($allText, '\[[LCK]\d+\]')
        Write-Host ("citations found: {0}" -f $m.Count)
    }
}

if ($Only -in @("all","draft")) {
    $d = Hit "3. contract-draft" "/legal/contract-draft" @{
        contractName   = "上海智云科技-张三 劳动合同"
        contractType   = "劳动合同"
        partyA         = "上海智云科技有限公司"
        partyB         = "张三"
        amount         = 180000
        duration       = "3年（试用期3个月）"
        requirements   = "试用期3个月，月薪1.5万元，包含保密条款与竞业限制条款"
    }
    if ($d) {
        Write-Host ("title: {0}, confidence: {1}" -f $d.title, $d.confidence)
        $content = $d.generatedContent
        Write-Host ("content length: {0}" -f ($content.Length))
        $rc = $d.retrievalContext
        Write-Host ("retrieval laws/cases/kb: {0} {1} {2}" -f $rc.laws.Count, $rc.cases.Count, $rc.knowledge.Count)
        if ($content.Length -gt 0) { Write-Host ("--- content head ---`n" + $content.Substring(0, [Math]::Min(400,$content.Length))) }
    }
}
