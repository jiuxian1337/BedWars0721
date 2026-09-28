# BedWars0721 · 经验起床

[![Build](https://github.com/jiuxian1337/BedWars0721/actions/workflows/build.yml/badge.svg)](https://github.com/jiuxian1337/BedWars0721/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/jiuxian1337/BedWars0721?color=blue&label=release)](https://github.com/jiuxian1337/BedWars0721/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/jiuxian1337/BedWars0721/total?color=green&label=downloads)](https://github.com/jiuxian1337/BedWars0721/releases)
[![Stars](https://img.shields.io/github/stars/jiuxian1337/BedWars0721?color=f5c542&label=stars)](https://github.com/jiuxian1337/BedWars0721/stargazers)
[![License](https://img.shields.io/github/license/jiuxian1337/BedWars0721?color=orange&label=license)](LICENSE)
[![Bilibili](https://img.shields.io/badge/B站-开服教程视频-00A1D6?logo=bilibili&logoColor=white)](https://www.bilibili.com/video/BV1jMaj69EGg/)
[![QQ群](https://img.shields.io/badge/QQ群-点击加入-12B7F5?logo=tencentqq&logoColor=white)](https://qm.qq.com/q/YUi9MZse8E)

### 把经验条上的等级，直接当钱花

一个 [BedWars1058](https://github.com/andrei1058/BedWars1058) 附属插件，也就是大家熟悉的「经验起床」玩法：**捡起资源直接涨等级，商店 / 升级 / 陷阱全部用等级结算**。

这里没有额外的货币系统 —— 你经验条上显示的那个数字，就是钱。攒着冲一波大的，还是当场换成装备，全看你自己。

- 🎯 **即插即用** —— 丢进 `plugins/` 就能跑，不用改服务端、不需要数据库
- 🔒 **只影响你指定的地图** —— 没加入列表的地图完全保持原版玩法，不污染其它模式
- ⚡ **底层注入** —— 直接改写 BedWars1058 的字节码，没有额外轮询和 API 绕路的开销

<p align="center">
  <a href="#下载与安装"><b>⬇️ 立即下载</b></a> &nbsp;·&nbsp;
  <a href="https://www.bilibili.com/video/BV1jMaj69EGg/"><b>📺 开服教程视频</b></a> &nbsp;·&nbsp;
  <a href="#交流与反馈"><b>💬 加入交流群</b></a>
</p>

---

## 下载与安装

**下载地址**：**[Releases · 最新版本](https://github.com/jiuxian1337/BedWars0721/releases/latest)** —— 在页面底部的 **Assets** 里点 `BedWars0721-1.0.jar` 直接下载，**免登录**。

> 想尝鲜开发中的改动？[Actions 最新构建](https://github.com/jiuxian1337/BedWars0721/actions/workflows/build.yml) 的 Artifacts 里有每次提交的产物（需要登录 GitHub 账号）。

**环境要求**

| | |
| --- | --- |
| 服务端 | Spigot / Paper 及其衍生端，1.8 ~ 1.13+（插件按 1.8.8 编译） |
| Java | **17 或更高** |
| 前置插件 | [BedWars1058](https://github.com/andrei1058/BedWars1058)（建议使用与本插件匹配的版本） |

**四步开起来**

1. 把 `BedWars0721-1.0.jar` 放进服务端的 `plugins/` 目录
2. 重启服务器（会自动生成 `plugins/BedWars0721/config.yml`）
3. 进游戏执行 `/bw0721 addxparena <地图名>`，把需要经验模式的地图加进去
4. 进这局地图，捡个铁锭看看等级有没有涨 ✅

> 更想看视频操作？→ [**经验起床战争开服教程（B 站）**](https://www.bilibili.com/video/BV1jMaj69EGg/)

---

## 玩法

### 资源 = 经验

在启用了经验模式的地图里，捡起地上的资源不会进背包，而是**直接加到你的等级上**，并伴随一声清脆的音效。比如捡起 1 个绿宝石，等级直接 +100。

| 资源 | 折算经验 |
| --- | --- |
| 铁锭 | 1 |
| 金锭 | 10 |
| 经验瓶 | 10 |
| 绿宝石 | 100 |

以上是默认汇率，服主可以自由修改（见下方[配置](#配置)）。没写在配置里的资源保持原样，仍然是普通物品。

### 商店 = 花经验

商店、团队升级、陷阱全部**从你的等级里扣**，价格显示也会换算成经验，买不起时提示的差额同样是经验。

> 小提示：升级 / 陷阱菜单里物品的说明文字仍会显示原始货币名，但**实际扣费和能否购买都按经验计算**，以能否购买为准。

### 死亡

- **普通死亡**（自家床还在，且击杀者在场）：击杀者**直接获得你当前的等级数**（你有多少级，他就涨多少级），你归零。
- **其它死亡**（掉虚空、没有击杀者等）：按「你的等级 ÷ 经验瓶价值」的数量，在你死亡的位置掉落经验瓶，你归零。捡回来就能把等级拿回来。
- **最终击杀**（床被破坏）：经验瓶掉落在该队的击杀掉落点。
- 原版死亡掉落的经验球会被屏蔽，不会凭空多出等级。

### 不影响其它模式

只有被加入列表的地图才启用经验模式，其它地图完全保持原版起床战争玩法。

---

## 命令与权限

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/bw0721 reload` | 重载配置文件 | `bw0721.command.reload` |
| `/bw0721 addxparena <地图>` | 把地图加入经验模式 | `bw0721.command.addxparena` |

- 地图名支持 Tab 补全，直接从 BedWars1058 的地图列表里选。
- 拥有 `bw.*` 或 `bw0721.*` 的玩家可以使用全部命令；权限默认只给 OP。

---

## 配置

配置文件：`plugins/BedWars0721/config.yml`，首次启动自动生成，改完执行 `/bw0721 reload` 立即生效。

| 配置项 | 说明 |
| --- | --- |
| `messages.prefix` | 插件消息前缀 |
| `messages.experience` | 经验模式商店中货币的显示文本 |
| `messages.experience-color` | 经验模式商店中货币的显示颜色 |
| `currency` | 可折算成经验的资源：材料名 → 每件折算的等级数 |
| `xp-arenas` | 启用经验模式的地图列表 |

**关于 `currency`**

- 值是 `0` 或整条删掉，该资源就恢复成普通物品，不会变成经验。
- 想关掉某种资源（比如保留钻石原版玩法），把它设为 `0` 即可。
- 材料名按服务端版本填写：1.8 ~ 1.12 的经验瓶是 `EXP_BOTTLE`，1.13+ 是 `EXPERIENCE_BOTTLE`。

**关于经验瓶**

经验瓶的汇率同时决定了死亡掉落经验瓶的数量，默认 `10` 表示「1 瓶 = 10 级」。改小会让经验瓶更值钱、掉得更少；改大则相反，但要留意数量过多时对服务端的影响。

---

## 常见问题

**捡了资源等级没变？**
确认这张地图已经用 `/bw0721 addxparena` 加入列表（或写在 `xp-arenas` 里），并已 `/bw0721 reload`。

**经验条的绿色进度条不涨，是正常的吗？**
正常。参与玩法的是**等级数字**，绿色进度条不参与计算，看数字就行。

**游戏里的等级会带回大厅吗？**
不会。BedWars1058 在进游戏时清空等级、退场时恢复，经验模式不会污染大厅数据。

**升级菜单里的价格看起来还是原来的货币？**
菜单的说明文字仍沿用原版写法，但购买判定和扣费都是按经验走的，价格显示在商店里已换算为经验。这是已知表现，不影响实际扣费。

**支持 1.7 或者 1.20+ 吗？**
没有测试过。插件按 1.8.8 编译、并直接作用于 BedWars1058 的内部实现，服务端或前置插件版本相差太大时可能失效 —— 遇到问题欢迎到群里反馈。

**能用在别的起床插件上吗？**
不能，只支持 BedWars1058。

---

## 支持这个项目

这个插件完全免费开源。如果它帮到了你：

- ⭐ **点一个 Star** —— 让更多服主能刷到这个插件，这是对作者最实在的支持
- 📺 把[开服教程视频](https://www.bilibili.com/video/BV1jMaj69EGg/)分享给需要的朋友或服主群
- 🐛 遇到 Bug 或有功能建议，欢迎[提 Issue](https://github.com/jiuxian1337/BedWars0721/issues)
- 💬 加入交流群，使用问题、开服求助都可以直接问

[![Star History Chart](https://api.star-history.com/svg?repos=jiuxian1337/BedWars0721&type=Date)](https://star-history.com/#jiuxian1337/BedWars0721&Date)

## 交流与反馈

- 💬 **QQ 群**：[点击链接加入群聊【WatchNeko】](https://qm.qq.com/q/YUi9MZse8E)
- 🧰 **开服 / 插件 / 反作弊交流群**：`1060682915`（搜索群号加入）
- 📺 **B 站**：[经验起床战争开服教程](https://www.bilibili.com/video/BV1jMaj69EGg/)
- 🐛 **Bug / 功能建议**：[GitHub Issues](https://github.com/jiuxian1337/BedWars0721/issues)

## 相关链接

- [BedWars1058](https://github.com/andrei1058/BedWars1058) —— 本插件依赖的核心起床战争插件
- [本项目仓库](https://github.com/jiuxian1337/BedWars0721)

---

## 开源协议

本项目采用 [GNU General Public License v3.0](LICENSE) 授权，与前置插件 BedWars1058 保持一致。

Copyright (C) 2026 jiuxian1337 (P01_4rU5er)

你可以自由使用、修改和分发本插件（包括开服使用和二次开发），但**分发修改后的版本时必须同样以 GPL-3.0 开源并附上源码**，同时保留版权声明。本插件不提供任何担保。
