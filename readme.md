# Thorns Rework

适用于 Minecraft **1.21.1**、Java **21** 和 Fabric。专服将 Thorns Rework 与适用于 1.21.1 的 Fabric API 一起放入 `mods/`。连接该专服的客户端可选装本模组。单人或局域网游戏由安装本模组的主机运行服务端逻辑。

## 构建

在本目录运行 Windows 命令 `gradlew.bat build`，或在 Linux/macOS 上运行 `./gradlew build`。发布 JAR 位于 `build/libs/thorns-rework-0.1.0-beta.jar`。构建版本锁定在 `gradle.properties`，本次使用 Fabric Loader 0.20.1、Fabric API 0.116.18+1.21.1、Yarn 1.21.1+build.3、Loom 1.11.8 和 Gradle 8.14。

## 游戏行为

| 情况 | 触发后的处理 |
| --- | --- |
| 创造模式攻击者 | 跳过攻击者处置 |
| 非生物攻击者或无攻击者 | 跳过攻击者处置，调试开启时记录原版触发判定结果 |
| 主手持有耐久物品 | 按触发装备的荆棘等级扣除 `3 × 等级` 点耐久，绕过耐久附魔。达到上限时按原版堆叠递减和装备破坏通知处理 |
| 主手为空 | 提交 1.0 至 5.0 的 `minecraft:thorns` 伤害，最终掉血由原版防御和受伤规则决定 |
| 主手持有普通无耐久物品 | 数量减 1 |
| 主手持有附魔书 | 掉落 1 本保留原物品组件的附魔书 |
| 主手带 `minecraft:unbreakable` | 保护该物品，跳过处置 |

反制只检查攻击者主手。有主人的弹射物按原版归属判定攻击者。骷髅射箭命中并触发荆棘时，检查骷髅当时的主手，通常是弓。

### 多件荆棘的耐久叠加

同一次攻击可以触发多件荆棘装备。每件装备按自己的荆棘等级独立判定，I、II、III 级的触发概率分别为 15%、30%、45%。每次触发都会重新检查攻击者主手，因此多次命中可以叠加扣除耐久。荆棘 I、II、III 每次触发分别扣 3、6、9 点。荆棘 I 和 III 同时触发时，攻击者主手的剑如果有足够耐久，就会合计损耗 12 点。

每件成功触发的荆棘装备也会各自承担 2 点基础耐久损耗。这部分沿用原版 `damage_item`，可受耐久附魔减免。

### 弹射物减免

弹射物伤害添加一次 8.333333 EPF，约等于 33.33% 减免，排除 `bypasses_invulnerability`。多件荆棘装备合计贡献一份减免，其他保护附魔和原版 80% 总上限继续生效。

## 困难难度精英僵尸和骷髅

仅原版生存难度为 Hard 时，原版自然生成的僵尸和骷髅各自进行一次独立出现概率判定，不影响指令、刷怪笼、增援及其它生成来源：

- **超越者（Beyond）**：默认独立出现概率 **5%**；穿戴 2–3 件不同部位的皮革、金质或锁链甲，每件装备随机拥有 I 或 II 级荆棘。
- **霸主（Overlord）**：默认独立出现概率 **0.2%**；穿戴 2–4 件不同部位的铁甲，每件均拥有 III 级荆棘。

精英出现时会替换这些盔甲部位原有的装备；程序化设置的盔甲死亡时不会掉落。若两次独立概率判定同时成功，同一生物按霸主档生成。

精英盔甲默认不会作为掉落物掉落。管理员（权限等级 2）开启 Debug Mode 后可用以下命令分别读取和修改概率；改动立即生效，并保存到 `config/thornsrework.json`：

```text
/thornsrework debug ThornMonsterSpawnRate
/thornsrework debug ThornMonsterSpawnRate Beyond change 5
/thornsrework debug ThornMonsterSpawnRate Overlord change 0.2
```

Beyond 和 Overlord 概率各自独立掷骰；各次生成由同一世界随机数生成器依次判定。两次同时命中时以装备更强的霸主为准。参数范围为 `0`–`100`。未开启调试模式时，命令显示 `需要使用/thornsrework debug set true开启调试模式 en-us:Use /thornsrework debug set true to enable debug mode.`。数值保存至 `config/thornsrework.json`，重启后继续生效。要用 JVM 参数强制开启 Debug Mode，可在专服的 `-jar` 前添加 `-Dthornsrework.debug=true`。

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
