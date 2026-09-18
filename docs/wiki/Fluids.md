# Fluids

## What a fluid becomes

A fluid dragged from JEI enters a recipe as an **AE2FC Fluid Packet**, not as a bucket.

That is what PackagedAuto itself uses. Its addon **PackagedFluidCrafting** injects into the
Encoder and turns a dragged `FluidStack` into `FakeFluids.packFluid2Packet(fluid)`. The Packager
and the Crafter read the packet back through its converting item handler.

A bucket in a recipe slot is an ordinary **item**. No fluid is ever moved for it, and the recipe
never runs. Earlier versions of this mod wrote a bucket, and that was wrong.

## What you need

Both mods, together:

- **AE2 Fluid Crafting**, which provides the packet item;
- **PackagedFluidCrafting**, which teaches the machines to read it.

Without them, a dragged fluid falls back to its filled bucket, because no machine could read a
packet anyway.

## Amounts

A fluid is edited in **millibuckets**, up to one billion.

| Where | Steps |
|---|---|
| Amount panel | `+10`, `+100`, `+1000` mB |
| Wheel | 100 mB, 1000 with Shift, 10000 with Ctrl |

The slot shows the amount in **buckets**, like every AE2 fluid screen: 4000 mB reads `4`, 250 mB
reads `0.25`, 120000 mB reads `120K`. The text shrinks on its own and never overflows the slot.

**Keep ratio** works across fluids and items together: a recipe that takes 1000 mB of water and
two items still takes 2000 mB and four.

## Gases

Not carried yet. The format exists in AE2 Fluid Crafting, under `ae2fc:gas_packet`, but it cannot
be verified in game without Mekanism, and this project does not write from memory.
