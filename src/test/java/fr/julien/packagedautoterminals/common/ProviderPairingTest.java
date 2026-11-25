package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.util.math.BlockPos;
import org.junit.Test;

/**
 * The grouping rules of the terminal, tested without the game.
 *
 * <p>These rules decide what the player sees on every row, and a mistake here shows the same
 * recipe twice, or hides a machine. Decision D38 fixes them.
 */
public class ProviderPairingTest {

    private static int nextX;

    /** A machine, at a position of its own, carrying the recipes named here. */
    private static ProviderSnapshot machine(ProviderRole role, String... recipes) {
        ProviderSnapshot snapshot = new ProviderSnapshot();
        snapshot.role = role;
        snapshot.pos = new BlockPos(nextX++, 64, 0);
        snapshot.name = role.name().toLowerCase();
        snapshot.holderPresent = true;
        for (String recipe : recipes) {
            snapshot.recipes.add(new FakeRecipe(recipe));
        }
        return snapshot;
    }

    private static List<ProviderPairing.Group> group(ProviderSnapshot... machines) {
        return ProviderPairing.group(new ArrayList<>(Arrays.asList(machines)));
    }

    @Test
    public void twoMachinesCarryingTheSameRecipesFormOneGroup() {
        List<ProviderPairing.Group> groups = group(
                machine(ProviderRole.PACKAGER, "iron", "gold"),
                machine(ProviderRole.UNPACKAGER, "iron", "gold"));

        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).machines.size());
        // The recipe is listed once, not once per machine.
        assertEquals(2, groups.get(0).recipes.size());
        assertTrue(groups.get(0).isPair());
    }

    @Test
    public void sharingOneRecipeIsNotEnoughToJoinAGroup() {
        // A package common to several pairs used to solder them into one blob of six
        // machines or more. Decision D38.
        List<ProviderPairing.Group> groups = group(
                machine(ProviderRole.PACKAGER, "iron", "gold"),
                machine(ProviderRole.UNPACKAGER, "iron", "diamond"));

        assertEquals(2, groups.size());
    }

    @Test
    public void aBrandNewPairWithTwoEmptyHoldersShowsOneRow() {
        List<ProviderPairing.Group> groups = group(
                machine(ProviderRole.PACKAGER),
                machine(ProviderRole.UNPACKAGER));

        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).machines.size());
    }

    @Test
    public void twoEmptyPackagersStayApartBecauseTheChoiceWouldBeAGuess() {
        List<ProviderPairing.Group> groups = group(
                machine(ProviderRole.PACKAGER),
                machine(ProviderRole.PACKAGER),
                machine(ProviderRole.UNPACKAGER));

        assertEquals(3, groups.size());
    }

    @Test
    public void anEmptyMachineJoinsTheGroupThatWaitsForExactlyItsRole() {
        // The player encoded the recipe in the Packager and not yet in the Unpackager. The
        // two share nothing, and would otherwise each complain about the other.
        List<ProviderPairing.Group> groups = group(
                machine(ProviderRole.PACKAGER, "iron"),
                machine(ProviderRole.UNPACKAGER));

        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).machines.size());
        // The group holds both roles, so nothing is missing at the group level.
        assertNull(ProviderPairing.missingRoleOf(groups.get(0)));
        // The recipe itself still sits on one side only.
        assertTrue(!groups.get(0).isComplete(new FakeRecipe("iron")));
    }

    @Test
    public void theNameGivenByThePlayerJoinsTwoGroupsThatShareNothing() {
        ProviderSnapshot packager = machine(ProviderRole.PACKAGER, "iron");
        ProviderSnapshot unpackager = machine(ProviderRole.UNPACKAGER, "gold");
        packager.customName = "ingots";
        unpackager.customName = "ingots";

        List<ProviderPairing.Group> groups = group(packager, unpackager);

        assertEquals(1, groups.size());
        assertEquals("ingots", groups.get(0).customName());
        assertEquals(2, groups.get(0).recipes.size());
    }

    @Test
    public void aGroupWithNoRecipeMissesNothing() {
        List<ProviderPairing.Group> groups = group(machine(ProviderRole.PACKAGER));
        assertNull(ProviderPairing.missingRoleOf(groups.get(0)));
    }

    @Test
    public void aLonePackagerWithARecipeMissesItsUnpackager() {
        List<ProviderPairing.Group> groups = group(machine(ProviderRole.PACKAGER, "iron"));
        assertEquals(ProviderRole.UNPACKAGER, ProviderPairing.missingRoleOf(groups.get(0)));
    }

    @Test
    public void aSelfSufficientMachineMissesNobody() {
        List<ProviderPairing.Group> groups = group(machine(ProviderRole.COMPLETE, "iron"));
        assertNull(ProviderPairing.missingRoleOf(groups.get(0)));
        assertTrue(groups.get(0).isComplete(new FakeRecipe("iron")));
    }

    @Test
    public void aMachineOfAnUnknownAddonRaisesNoDiagnostic() {
        List<ProviderPairing.Group> groups = group(machine(ProviderRole.UNKNOWN, "iron"));
        assertNull(ProviderPairing.missingRoleOf(groups.get(0)));
        assertTrue(groups.get(0).isComplete(new FakeRecipe("iron")));
    }

    @Test
    public void theLonePartnerIsFoundWhenTheNetworkHoldsOnlyOne() {
        ProviderSnapshot packager = machine(ProviderRole.PACKAGER, "iron");
        ProviderSnapshot unpackager = machine(ProviderRole.UNPACKAGER, "gold");
        List<ProviderSnapshot> all = new ArrayList<>(Arrays.asList(packager, unpackager));

        ProviderPairing.Group group = ProviderPairing.group(all).get(0);
        ProviderSnapshot found = ProviderPairing.findLonePartner(
                all, group, ProviderRole.UNPACKAGER);

        assertSame(unpackager, found);
    }

    @Test
    public void thePartnerStaysUnknownWhenSeveralCouldDo() {
        ProviderSnapshot packager = machine(ProviderRole.PACKAGER, "iron");
        List<ProviderSnapshot> all = new ArrayList<>(Arrays.asList(packager,
                machine(ProviderRole.UNPACKAGER, "gold"),
                machine(ProviderRole.UNPACKAGER, "diamond")));

        ProviderPairing.Group group = ProviderPairing.group(all).get(0);

        assertNull(ProviderPairing.findLonePartner(all, group, ProviderRole.UNPACKAGER));
    }

    @Test
    public void aMachineIsFoundBackByItsPosition() {
        ProviderSnapshot packager = machine(ProviderRole.PACKAGER, "iron");
        List<ProviderPairing.Group> groups = group(packager);

        assertNotNull(ProviderPairing.groupOf(groups, packager.dimension, packager.pos));
        assertNull(ProviderPairing.groupOf(groups, packager.dimension, new BlockPos(999, 0, 0)));
    }

    @Test
    public void aHolderListingTheSameRecipeTwiceStillPairsWithItsPartner() {
        // A recipe holder can list a duplicate. The comparison counts the distinct recipes.
        List<ProviderPairing.Group> groups = group(
                machine(ProviderRole.PACKAGER, "iron", "iron"),
                machine(ProviderRole.UNPACKAGER, "iron"));

        assertEquals(1, groups.size());
        assertEquals(1, groups.get(0).recipes.size());
    }

    @Test
    public void aHealthyPairOffersNoFix() {
        // The button of this row used to send every holder of the group back to the network.
        List<ProviderPairing.Group> groups = group(
                machine(ProviderRole.PACKAGER, "iron"),
                machine(ProviderRole.UNPACKAGER, "iron"));

        assertEquals(ProviderPairing.Fix.NONE, groups.get(0).fix());
    }

    @Test
    public void aMachineWithoutHolderIsOfferedOne() {
        ProviderSnapshot unpackager = machine(ProviderRole.UNPACKAGER);
        unpackager.holderPresent = false;

        List<ProviderPairing.Group> groups = group(machine(ProviderRole.PACKAGER), unpackager);

        assertEquals(ProviderPairing.Fix.INSERT_HOLDERS, groups.get(0).fix());
    }

    @Test
    public void aDriftedGroupHoldingBothRolesIsOfferedTheRepair() {
        ProviderSnapshot packager = machine(ProviderRole.PACKAGER, "iron", "gold");
        ProviderSnapshot unpackager = machine(ProviderRole.UNPACKAGER, "iron");
        packager.customName = "ingots";
        unpackager.customName = "ingots";

        assertEquals(ProviderPairing.Fix.REPAIR, group(packager, unpackager).get(0).fix());
    }

    @Test
    public void aSplitPairIsNotOfferedTheRepair() {
        // Each row holds one role. The repair copies into the machines of its own row, which
        // already carry the recipes, and it wrote nothing.
        List<ProviderPairing.Group> groups = group(
                machine(ProviderRole.PACKAGER, "iron", "gold"),
                machine(ProviderRole.UNPACKAGER, "iron"));

        assertEquals(2, groups.size());
        assertFalse(groups.get(0).isComplete(new FakeRecipe("iron")));
        assertEquals(ProviderPairing.Fix.NONE, groups.get(0).fix());
        assertEquals(ProviderPairing.Fix.NONE, groups.get(1).fix());
    }
}
