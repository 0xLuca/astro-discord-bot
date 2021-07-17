package at.zieserl.astrodiscordbot.constant;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public final class Roles {
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
