# Thorns Rework

适用于 Minecraft **1.21.1**、Java **21** 和 Fabric。专服将 Thorns Rework 与适用于 1.21.1 的 Fabric API 一起放入 `mods/`。连接该专服的客户端可选装本模组。单人或局域网游戏由安装本模组的主机运行服务端逻辑。

## 构建

在本目录运行 Windows 命令 `gradlew.bat build`，或在 Linux/macOS 上运行 `./gradlew build`。发布 JAR 位于 `build/libs/thorns-rework-1.0.0.jar`。构建版本锁定在 `gradle.properties`，本次使用 Fabric Loader 0.20.1、Fabric API 0.116.18+1.21.1、Yarn 1.21.1+build.3、Loom 1.11.8 和 Gradle 8.14。

## 游戏行为

| 情况 | 触发后的处理 |
| --- | --- |
| 创造模式攻击者 | 跳过攻击者处置 |
| 非生物攻击者或无攻击者 | 跳过攻击者处置，调试开启时记录原版触发判定结果 |
| 主手持有耐久物品 | 扣 3 点耐久，绕过耐久附魔。达到上限时按原版堆叠递减和装备破坏通知处理 |
| 主手为空 | 提交 1.0 至 5.0 的 `minecraft:thorns` 伤害，最终掉血由原版防御和受伤规则决定 |
| 主手持有普通无耐久物品 | 数量减 1 |
| 主手持有附魔书 | 掉落 1 本保留原物品组件的附魔书 |
| 主手带 `minecraft:unbreakable` | 保护该物品，跳过处置 |

反制只检查主手。有主人的弹射物按原版归属判定攻击者，骷髅命中目标且触发荆棘时检查骷髅当前的主手，通常是弓。荆棘仍逐件独立判定。持有者的 2 点装备损耗作用在本次触发的装备上，沿用原版 `damage_item` 和耐久附魔减免。

弹射物伤害添加一次 8.333333 EPF，约等于 33.33% 减免，排除 `bypasses_invulnerability`。多件荆棘装备仍只贡献一份，其他保护附魔和原版 80% 总上限继续生效。

## 调试与冲突

- `config/thornsrework.json` 默认为 `{"debug": false}`。
- `/thornsrework debug status` 查询开关、配置路径和游戏参数。
- `/thornsrework debug set true` 或 `false` 修改并保存开关，`/thornsrework debug reload` 重新读取配置。命令需要权限等级 2。
- 专服可在 `-jar` 前加 `-Dthornsrework.debug=true` 强制开启。调试信息同时发送到玩家聊天和服务端日志。
- 服务器启动和数据包重载时检查 `minecraft:thorns` 的来源。报告会列出冲突包、检测到的资源来源和实际优先来源。移除报告中的冲突数据包或模组后重启服务器。

本体通过内置数据包和 Java 自定义附魔效果执行。三个 Mixin 分别观察原版概率结果、让荆棘 EPF 每次只计入一份，以及在登录同步时从发给客户端的荆棘记录中省略服务端效果。

## 鸣谢
gpt-6.2-sol（开发）
deepseek-v4.1-flash（企划）
[需求追踪和测试记录](docs/verification.md)
