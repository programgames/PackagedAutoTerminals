# Fluids

## What a fluid becomes

A fluid dragged out of JEI enters a recipe as an AE2FC Fluid Packet. A Fluid Packet is an item
that carries one fluid. It is not a bucket.

PackagedAuto alone refuses a fluid. Its addon PackagedFluidCrafting changes the Encoder with a
mixin. A mixin is code that one mod adds into the class of another mod. The addon turns a dragged
`FluidStack` into `FakeFluids.packFluid2Packet(fluid)`. The Packager and the Crafter read the
fluid back out of the packet.

A bucket in a recipe slot stays an ordinary item. No fluid moves for it, and the recipe never
runs. Earlier versions of this mod wrote a bucket, and that was wrong.

## What you need

The fluids need two mods. AE2 Fluid Crafting provides the packet item. PackagedFluidCrafting
teaches the machines to read the packet. Install both.

Without them, a dragged fluid becomes its filled bucket, because no machine reads a packet anyway.

## Amounts

You edit a fluid in millibuckets (mB), up to one billion.

| Where | Steps |
|---|---|
| Amount panel | `+10`, `+100`, `+1000` mB |
| Wheel | 100 mB, 1000 mB with Shift, 10000 mB with Ctrl |

The slot shows the amount in buckets, like every other AE2 fluid screen. 4000 mB reads `4`, 250 mB
reads `0.25`, and 120000 mB reads `120K`. The text shrinks on its own and never overflows the
slot.

Keep ratio works across fluids and items together. A recipe that takes 1000 mB of water and two
items becomes a recipe that takes 2000 mB and four items.

## Gases

Gases do not pass yet. The format exists in AE2 Fluid Crafting, under `ae2fc:gas_packet`. Nobody
verified it in game without Mekanism. This project writes nothing from memory.
