# 流体

## 流体会变成什么

从 JEI 拖入配方的流体会变成 AE2FC 的 Fluid Packet。Fluid Packet 是一种承载流体的物品。它不是桶。

PackagedAuto 本身不接受流体。其附属模组 PackagedFluidCrafting 用 mixin 修改编码器。mixin 是
一个模组加入另一个模组类中的代码。该附属模组把拖入的 `FluidStack` 转成
`FakeFluids.packFluid2Packet(fluid)`。封包机和合成器再从封包中读回流体。

配方格中的桶只是一个普通物品。不会有任何流体为它移动，配方也就永远不会运行。本模组的早期版本写入
的是桶，那是错的。

## 需要什么

流体需要两个模组。AE2 Fluid Crafting 提供封包物品。PackagedFluidCrafting 教会机器读取封包。请把
两个都装上。

没有它们时，拖入的流体会变成对应的满桶，因为无论如何都没有机器能读封包。

## 数量

你以毫桶（mB）为单位编辑流体，上限十亿。

| 位置 | 步长 |
|---|---|
| 数量面板 | `+10`、`+100`、`+1000` mB |
| 滚轮 | 100 mB，按住 Shift 为 1000 mB，按住 Ctrl 为 10000 mB |

格子按桶显示数量，与 AE2 的其他流体界面一致。4000 mB 显示 `4`，250 mB 显示 `0.25`，120000 mB
显示 `120K`。文字会自动缩小，绝不会超出格子。

Keep ratio 同时处理流体与物品。一个需要 1000 mB 水和两个物品的配方，会变成需要 2000 mB 和四个
物品。

## 气体

气体尚未支持。AE2 Fluid Crafting 中确实存在 `ae2fc:gas_packet` 格式。没有 Mekanism 时没有人在
游戏中验证过它。本项目从不凭记忆写任何内容。
