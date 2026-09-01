# MuMu 公主连结 ABI 修复工具（定点修改版）

MuMu 模拟器更新后，公主连结被误用 x86 启动导致卡顿。本工具以 root 权限，把公主连结的 ABI 选择改为 `x86_64`。

## 这个工具做了什么（对文件的操作）

以 root 读取 MuMu 内 `/data/system/etc/mumu-configs/` 下的两个配置文件：
- `abi-select-android12.config`
- `abi-select-v2.config`

执行的操作：
1. **读取**上述两个文件（若不存在则提示）。
2. **备份**：修改前把原文件复制为 `<文件名>.bak`（仅当 `.bak` 不存在时执行，保留最初原件）。
3. **定点修改**：**只**把公主连结相关条目 `com.bilibili.priconne`（含 `.yofun.mumu` 渠道版）的 ABI 改为 **`x86_64`**。
4. **其余条目原样保留**：台服 `tw.sonet.princessconnect`、其它游戏均不改动。
5. **还原备份**：用 `.bak` 一键恢复修改前的文件。

## 使用前提（重要）

- **MuMu 模拟器必须已开启 root**：在 MuMu 设置中打开「root 权限」。目标目录 `/data/system/etc/mumu-configs` 是 MuMu 专用路径。
- **打开本软件后需手动提权**：本软件**不会自动申请 root**。MuMu 通过 **KernelSU** 提权——打开 App 后，请在 **KernelSU 管理器 → 超级用户（Superuser）列表**里**手动允许/授予**本软件 root 权限（或直接在授权弹窗里点允许）。

## 使用方法

### 方式一：Android App（推荐）
1. 把 `MumuAbiFix.apk` 安装进**已开 root** 的 MuMu。
2. 打开 App，在 root 授权框里**手动允许/提权**。
3. 点「**定点修复（备份原件）**」→ 以 root 备份并只修改公主连结条目为 `x86_64`。
4. **重启 MuMu 模拟器**使配置生效。
5. 出问题点「**还原备份**」恢复。

### 方式二：adb 脚本（无需装 App）
1. 电脑安装 adb；MuMu 已开「ADB 调试」+ root。
2. 改 `adb-fix.bat` 顶部的 MuMu adb 地址:端口（MuMu 设置→ADB调试 中查看），双击运行。
3. `adb-fix.bat`：备份并定点修改；`adb-fix.bat restore`：还原；`adb-fix.bat status`：查看。

## 文件清单
- `app/`：Android 工程（Kotlin，定点修复逻辑）。
- `adb-fix.bat` + `fix_edit.ps1`：Windows 上的 adb 一键脚本（定点修改）。
- `abi-select-android12.config` / `abi-select-v2.config`：修复后的参考配置。
- `.github/workflows/build.yml`：GitHub Actions 云端自动打 APK（含签名 release，密钥存 GitHub Secrets）。