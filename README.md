# ParkourArea

一个面向 Paper 1.21+（同时兼容 **Folia**）的跑酷区域插件。

玩家进入跑酷全局区域时自动切换为「跑酷玩家」状态（切换游戏模式、替换快捷工具栏、启用计时/存档/评级），
离开区域自动恢复原状态。支持区域层级划分、关卡计时与最佳记录、降星评级、选关/删档、中途存档点、
方块踩踏命令、防挂机，以及 WorldEdit/FAWE、PlaceholderAPI 软依赖。

## 特性

- **Paper + Folia 双兼容**：直接使用 paper-api 暴露的 region/async/global/entity 调度器，一套代码两端运行。
- **区域层级**：全局 ⊃ [大厅, 关卡 [起点 & 终点]]，自动校验父子关系与几何包含；支持 cuboid 与 sphere 选区。
- **关卡计时**：每关记录前 10 次通关用时与最佳用时。
- **用时排行榜**：总览页按 GLOBAL 区域平铺关卡（lore 显示我的最佳 + 全服前 3，翻页 + 区域切换），
  点击关卡查看详细（我的最佳 / 最近 10 次 / 全服第 1-10 / 第 11-20 名）。
- **降星评级**：3 档时间阈值降星，超时闪烁（每 500ms 交替）。
- **选关/删档**：按所在全局区域过滤关卡，灰/黄/绿混凝土区分关卡状态，玩家只能选非灰色关卡；删档后限制选择。
- **中途存档点**：默认踩金块记录，actionbar 持续提示，通关/重玩/返回大厅/离开清除。
- **方块踩踏命令**：配置踩指定方块执行命令/音效，支持 repeat 与 %player% / PlaceholderAPI 变量。
- **防挂机**：60s 无移动或视角变化作废成绩并传回大厅。
- **跳关检测**：未按顺序游玩自动返回大厅，不计成绩。
- **快捷工具栏**：进入区域时替换热栏 9 格（重玩/返回大厅/回存档点/菜单），离开或进入编辑模式恢复。
- **状态对账**：跑酷区域中切到非指定游戏模式或开启编辑模式会自动退出跑酷状态（保留当前游戏模式），条件恢复自动回到跑酷状态。

## 安装

1. 将 `ParkourArea-*.jar` 放入服务端 `plugins/` 目录。
2. 启动服务端，自动生成默认配置（`config.yml`、`messages.yml`、`blocks.yml`、`ratings.yml`、`zones.yml`）与 SQLite 数据库 `parkour.db`。
3. 可选：安装 [WorldEdit](https://modrinth.com/plugin/worldedit) / [FastAsyncWorldEdit](https://modrinth.com/plugin/fastasyncworldedit) 以使用选区创建区域，安装 [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) 以使用变量。

## 命令

主命令 `/parkour`（别名 `pko`、`pk`、`po`）。`*` 表示需要 `parkour.admin` 或 `parkour.edit` 权限。

| 命令 | 说明 |
|---|---|
| `/parkour` | 显示帮助 |
| `/parkour gui` | 打开主菜单 |
| `/parkour editmode [true\|false]` | *切换编辑模式 |
| `/parkour create <type> <name\|-> [parent\|-\|here] [cuboid\|sphere] <coords...>` | *创建区域 |
| `/parkour delete <zoneid/zonename>` | *删除区域（二次确认，级联删除子区域） |
| `/parkour edit <zoneid/zonename> rename <newname>` | *重命名区域 |
| `/parkour edit <zoneid/zonename> spawn [here]` | *取当前位置与朝向为传送点（留空默认 here） |
| `/parkour edit <zoneid/zonename> spawn <x> <y> <z> <yaw> <pitch>` | *设置区域传送点（全量） |
| `/parkour edit <zoneid/zonename> spawn yaw <yaw> [pitch <pitch>]` | *仅设置传送朝向 |
| `/parkour edit <zoneid/zonename> spawn clear` | *清除区域传送点 |
| `/parkour edit <zoneid/zonename> resize <x1> <y1> <z1> <x2> <y2> <z2>` | *重设 CUBOID 区域范围（可省略坐标用 WE 选区） |
| `/parkour edit <zoneid/zonename> resize <cx> <cy> <cz> <radius>` | *重设 SPHERE 区域范围 |
| `/parkour info [zoneid/zonename \| pos <x> <y> <z>]` | 显示单个区域信息 |
| `/parkour info-all [zoneid/zonename \| pos <x> <y> <z>]` | 显示当前位置所有关系区域 |
| `/parkour list` | 列出所有区域 |
| `/parkour togglesound [all\|checkpoint\|block] [on\|off]` | 切换跑酷音效（选项留空默认 all，on/off 留空为切换） |
| `/parkour resetsave <levelzone>` | 删除玩家在某关的存档（二次确认） |
| `/parkour reload` | *重载全部配置 |
| `/parkour updateconf` | *更新过旧配置文件（备份 .bak 并合并新增配置项） |
| `/parkour recreateconf` | *重建全部配置文件（备份 .bak，二次确认；不含 zones.yml） |

### create 参数说明

- `<type>`：`global` / `lobby` / `level` / `start` / `end`
- `<name|->`：区域名称，`-` 表示留空
- `[parent|-|here]`：父区域名称或 `#id`；`-` 表示无父级（仅 GLOBAL）；`here` 从当前位置推断；**留空默认 here**
- `[cuboid|sphere]`：选区模式（sphere 仅 start/end 可用），默认 cuboid
- `<coords>`：cuboid 为 `<x1> <y1> <z1> <x2> <y2> <z2>`；sphere 为 `<cx> <cy> <cz> <radius>`。安装了 WorldEdit 且 cuboid 时可省略坐标，改用当前选区。

示例：
```
/parkour editmode true
/parkour create global myparkour - cuboid -1000 -64 -1000 1000 320 1000
/parkour create lobby 大厅          # 站在全局区域内，parent 留空自动推断，用 WE 选区
/parkour create level level1 cuboid 0 100 0 30 130 30
/parkour create start start1 #4 sphere 15 120 15 1.5
```

## 区域层级规则

- 优先级：起点 = 终点 > 关卡 > 大厅 > 全局
- 同世界可创建多个全局区域，互不几何相交；子区域必须完全几何包含于父级且不与同级相交
- 起点与终点可作为同一区域使用：若关卡未单独创建 END，玩家在 RUNNING 阶段踩 START 即算通关

## 配置

详见各配置文件头部注释。关键项：

- `config.yml`：检测周期、防挂机阈值、存档点方块、要求游戏模式、传送朝向（`settings.teleport-keep-rotation`）等
- `blocks.yml`：踩方块触发的命令/音效
- `ratings.yml`：每关降星评级（以关卡区域 ID 为 key）
- `messages.yml`：全部文案（兼容 legacy `&` 码与 MiniMessage）
- `zones.yml`：区域定义（通常用命令管理，也可手动编辑后 `/parkour reload`）

### 传送朝向

`settings.teleport-keep-rotation`（默认 `true`）：传送（回大厅/选关起点/重玩/回存档点）时，
区域 spawn 未手动指定 yaw/pitch 的字段保留玩家当前朝向；设为 `false` 则未指定字段回落 `0/0`。
手动指定朝向用 `/parkour edit <zone> spawn yaw <yaw> [pitch <pitch>]`（大厅挂 LOBBY 区域、
关卡朝向挂 LEVEL 区域）。

### 配置文件版本管理

`config.yml` / `messages.yml` / `blocks.yml` / `ratings.yml` 各自带版本键（`config-version` 等）。
插件升级后若数据目录中的版本低于 jar 内置版本，启动/重载时控制台警告，并向在线及进服的
`parkour.admin` 管理员逐文件提示。处理方式：

- `/parkour updateconf`：把过旧文件备份为 `<文件名>.bak`（已存在则 `.bak1`、`.bak2` 递增），
  再以 jar 新版为底合并——你的修改保留，新增配置项自动补入；文件损坏无法合并时提示重建。
- `/parkour recreateconf`：二次确认后把四个配置文件全部备份为 `.bak` 并从 jar 重建。
  **不影响 `zones.yml`（区域数据）与 `parkour.db`（数据库）。**

## 权限

| 节点 | 默认 | 说明 |
|---|---|---|
| `parkour.*` | op | 全部权限 |
| `parkour.admin` | op | 创建/删除/编辑/重载 |
| `parkour.edit` | op | 编辑模式 |
| `parkour.user` | true | 游玩、打开菜单 |

## PlaceholderAPI 变量

- `%parkour_best_<levelId>%`：该关最佳用时
- `%parkour_status_<levelId>%`：该关状态（NONE/VISITED/COMPLETED）
- `%parkour_completed_count%`：已通关关卡数

## Folia 兼容

`plugin.yml` 已声明 `folia-supported: true`。所有定时任务（每 2tick 区域检测、actionbar 渲染、防挂机、
repeat 方块命令）与玩家状态变更（背包/传送/音效）均通过 paper-api 暴露的四套调度器派发到正确线程。
SQLite 访问全部在 async 线程执行，不阻塞区域线程。

## 构建

```bash
./gradlew shadowJar
```

产物位于 `build/libs/ParkourArea-*.jar`。
