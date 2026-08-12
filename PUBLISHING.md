# npm 发布指南

本仓库提供本地一键 npm 发版命令。发版会创建版本提交和 Git tag，但**不会**自动推送 Git，也不会在检查失败后执行推送。

## 登录 npm

首次发布或登录状态失效时运行：

```bash
npm run npm:login
npm whoami
```

登录和发布均固定使用 npm 官方 registry：`https://registry.npmjs.org/`。

## 发布前预览

先在不修改版本、不创建 tag、不发布的情况下运行完整校验：

```bash
npm run release:check
npm run publish:dry-run
```

`release:check` 会在校验前后都确认 Git 工作树完全干净，并依次执行：

- `npm run typecheck`
- `npm test`
- `npm run build`
- `npm pack --dry-run`

工作树中任何已修改、暂存或未跟踪文件都会使检查失败。每个 `release:*` 命令都先执行该校验；`preversion` 生命周期还会在 `npm version` 写入版本号前再次执行工作树检查。因此，即使构建或打包意外改动了受 Git 管理的文件，版本提交和 tag 也会被阻止。

`publish:dry-run` 会根据当前版本选择 dist-tag：alpha 预发布使用 `alpha`，稳定版使用 `latest`。因此它既能预览当前 `2.0.0-alpha.0`，也能在稳定版发版前复用。

## 一键稳定版发布

确认发布内容已提交后，选择语义化版本：

```bash
npm run release:patch
npm run release:minor
npm run release:major
```

每个命令都会先运行完整发布校验，然后计算明确的下一个稳定版并交给 `npm version` 创建版本提交和 tag，随后将包以 `public` access 发布到官方 registry。稳定版没有指定 dist-tag，因此 npm 将其标记为 `latest`。

若 npm 账户启用了 2FA，可以把 OTP 传给发布命令：

```bash
npm run release:patch -- --otp=123456
```

也可在已创建版本后单独运行 `npm run publish:npm -- --otp=123456`。

## Alpha 预发布

当前版本是 alpha 时，使用下面的命令递增预发布版本并发布：

```bash
npm run release:alpha
```

它等价于以 `alpha` 作为预发布标识运行 `npm version prerelease --preid=alpha`，再以 npm 的 `alpha` dist-tag 发布。当前的 `2.0.0-alpha.0` 会递增为 `2.0.0-alpha.1`。用户需要显式安装 `expo-smartrefreshlayout@alpha`；alpha 包不会覆盖 `latest`。

从 alpha 发布稳定版时，`release:patch` 会提升同一基线版本；以当前 `2.0.0-alpha.*` 为例，`release:patch`、`release:minor` 和 `release:major` 分别发布 `2.0.0`、`2.1.0` 和 `3.0.0`。版本已稳定后，这三个命令再按常规语义递增 patch、minor 或 major。稳定版会发布到 `latest`。

## 发布后

发布成功后，检查版本和 tag，然后手动推送提交及 tag：

```bash
git status
git push --follow-tags
```

不要在发布前推送新 tag。npm 发布是不可覆盖的；若发布后发现问题，应修复后发布新的版本。
