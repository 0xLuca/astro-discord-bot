package at.zieserl.astrodiscordbot.constant;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public final class Roles {
    public static final String AUSSER_DIENST_ID = "851109998569979911";
    public static final String AZUBI_ID = "851109998549532699";
    public static final String ABTEILUNGEN_ID = "851109998549532696";
    public static final String RETTUNGSMEDIZIN_ID = "851109998518992913";
    public static final String SONSTIGES_ID = "851109998518992910";
    public static final String LSMD_ID = "851109998486487091";

    public static void grantRole(Member member, String roleId) {
        Guild guild = member.getGuild();
        Role role = guild.getRoleById(roleId);
        assert role != null : "Requested role grant with unknown roleId";
        guild.addRoleToMember(member, role).queue();
    }

    public static void removeRole(Member member, String roleId) {
        Guild guild = member.getGuild();
        Role role = guild.getRoleById(roleId);
        assert role != null : "Requested role remove with unknown roleId";
        guild.removeRoleFromMember(member, role).queue();
    }
}
