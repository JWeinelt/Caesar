package de.julianweinelt.caesar.endpoint.plugins;


/**
 * A list of supported Minecraft server software. Some of them might be discontinued or experimental.
 */
public enum MinecraftServerSoftware {
    SPIGOT,
    @MCSLegacy
    CRAFTBUKKIT,
    PAPERMC,
    PURPUR,
    LEAFMC,
    PUFFERFISH,
    FOLIA,

    @MCSExperimental
    MINESTOM,


    SPONGE,
    @MCSExperimental
    FORGE,
    FABRIC,
    @MCSExperimental
    MAGMA,
    @MCSExperimental
    MOHIST,
    @MCSExperimental
    ARCLIGHT,
    @MCSExperimental
    CARDBOARD,

    @MCSLegacy
    BUNGEECORD,
    @MCSLegacy
    WATERFALL,
    VELOCITY
}
