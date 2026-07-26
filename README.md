# ParkourArea

一个面向 Paper 1.21+（同时兼容 **Folia**）的跑酷区域插件。

玩家进入跑酷全局区域时自动切换为「跑酷玩家」状态（切换游戏模式、替换快捷工具栏、启用计时/存档/评级），
离开区域自动恢复原状态。支持区域层级划分、关卡计时与最佳记录、降星评级、选关/删档、中途存档点、
方块踩踏命令、防挂机，以及 WorldEdit/FAWE、PlaceholderAPI 软依赖。

## 特性

- **Paper + Folia 双兼容**：直接使用 paper-api 暴露的 region/async/global/entity 调度器，一套代码两端运行。
- **区域层级**：全局 ⊃ [大厅, 关卡 [起点 & 终点]]，自动校验父子关系与几何包含；支持 cuboid 与 sphere 选区。
- **关卡计时**：每关记录前 10 次通关用时与最佳用时，GUI 展示。
- **降星评级**：3 档时间阈值降星，超时闪烁（每 500ms 交替）。
- **选关/删档**：灰/黄/绿混凝土区分关卡状态，玩家只能选非灰色关卡；删档后限制选择。
- **中途存档点**：默认踩金块记录，actionbar 持续提示，通关/重玩/返回大厅/离开清除。
- **方块踩踏命令**：配置踩指定方块执行命令/音效，支持 repeat 与 %player% / PlaceholderAPI 变量。
- **防挂机**：60s 无移动或视角变化作废成绩并传回大厅。
- **跳关检测**：未按顺序游玩自动返回大厅，不计成绩。
- **快捷工具栏**：进入区域时替换热栏 9 格（重玩/返回大厅/回存档点/菜单），离开或进入编辑模式恢复。

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
| `/parkour create <type> <name\|-> <parent\|-\|here> [cuboid\|sphere] <coords...>` | *创建区域 |
| `/parkour delete <zoneid/zonename>` | *删除区域（二次确认，级联删除子区域） |
| `/parkour edit <zoneid/zonename> rename <newname>` | *编辑区域 |
| `/parkour info [zoneid/zonename \| pos <x> <y> <z>]` | 显示单个区域信息 |
| `/parkour info-all [zoneid/zonename \| pos <x> <y> <z>]` | 显示当前位置所有关系区域 |
| `/parkour list` | 列出所有区域 |
| `/parkour resetsave <levelzone>` | 删除玩家在某关的存档（二次确认） |
| `/parkour reload` | *重载全部配置 |

### create 参数说明

- `<type>`：`global` / `lobby` / `level` / `start` / `end`
- `<name|->`：区域名称，`-` 表示留空
- `<parent|-|here>`：父区域 id 或名称；`-` 表示无父级（仅 GLOBAL）；`here` 自动从当前位置推断
- `[cuboid|sphere]`：选区模式（sphere 仅 start/end 可用），默认 cuboid
- `<coords>`：cuboid 为 `<x1> <y1> <z1> <x2> <y2> <z2>`；sphere 为 `<cx> <cy> <cz> <radius>`。安装了 WorldEdit 且 cuboid 时可省略坐标，改用当前选区。

示例：
```
/parkour editmode true
/parkour create global myparkour - cuboid -1000 -64 -1000 1000 320 1000
/parkour create lobby 大厅 1 here cuboid 10 100 10 50 150 50
/parkour create level level1 2 here cuboid 0 100 0 30 130 30
/parkour create start start1 3 here sphere 15 120 15 1.5
```

## 区域层级规则

- 优先级：起点 = 终点 > 关卡 > 大厅 > 全局
- 全局区域每世界唯一；子区域必须完全几何包含于父级且不与同级相交
- 起点与终点可作为同一区域使用：若关卡未单独创建 END，玩家在 RUNNING 阶段踩 START 即算通关

## 配置

详见各配置文件头部注释。关键项：

- `config.yml`：检测周期、防挂机阈值、存档点方块、要求游戏模式等
- `blocks.yml`：踩方块触发的命令/音效
- `ratings.yml`：每关降星评级（以关卡区域 ID 为 key）
- `messages.yml`：全部文案（兼容 legacy `&` 码与 MiniMessage）
- `zones.yml`：区域定义（通常用命令管理，也可手动编辑后 `/parkour reload`）

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
