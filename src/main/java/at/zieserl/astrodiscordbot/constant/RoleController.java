package at.zieserl.astrodiscordbot.constant;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public final class RoleController {
    public static void grantRole(final Member member, final String roleId) {
        final Guild guild = member.getGuild();
        final Role role = guild.getRoleById(roleId);
        assert role != null : "Requested role grant with unknown roleId";
        guild.addRoleToMember(member, role).queue();
    }

    public static void removeRole(final Member member, final String roleId) {
        final Guild guild = member.getGuild();
        final Role role = guild.getRoleById(roleId);
        assert role != null : "Requested role remove with unknown roleId";
        guild.removeRoleFromMember(member, role).queue();
    }
}