# MuMu 公主连结(ABI)卡顿修复工具 —— 定点修改版

背景: MuMu 模拟器更新后, 公主连结被错误地用 x86 启动导致卡顿。

## 本工具做什么
**定点修改**: 以 root 读取 `/data/system/etc/mumu-configs` 下的两个配置文件, 只把
`com.bilibili.priconne`(国服/官服, 含 `.yofun.mumu` 渠道版)的 ABI 改为 `x86_64`。
**台服 tw.sonet.princessconnect、其它游戏、其它行全部原样保留**。
修改前把原文件备份为 `.bak`, 可一键还原。

## 两种使用方式
### 方式一: Android App(推荐, 已打包好 APK)
- App 已构建为**签名 release APK**, 位置见 `app/build/outputs/apk/release/app-release.apk`。
- 装进已 root 的 MuMu → 打开 App → 点「**定点修复（备份原件）**」→ 重启模拟器生效。
- 出问题点「**还原备份**」。App 不主动申请 root。

### 方式二: adb 一键脚本(无需装 App)
- `adb-fix.bat`(含 `fix_edit.ps1`)—— 改好 MuMu adb 端口后双击运行, 同样是定点修改。
- `adb-fix.bat restore` 还原; `adb-fix.bat status` 查看。

## 重新打包 APK
```bat
set ANDROID_HOME=C:\Users\qiujunli\Documents\android-sdk
C:\Users\qiujunli\Documents\android-build\gradle-8.7\bin\gradle.bat assembleRelease
```
签名密钥库: `release.keystore`, 密码 `abifix123`, 别名 `abifix`(保管好, 更新需同一签名)。

## 提示
- 只对 MuMu 生效(目标目录是 MuMu 专用路径)。
- 若设备上目标文件没有公主连结条目, App/脚本会提示「未找到, 未改动」。
- 覆盖后需重启 MuMu 模拟器使配置生效。