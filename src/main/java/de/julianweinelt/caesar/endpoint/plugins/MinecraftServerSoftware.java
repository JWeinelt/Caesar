package de.julianweinelt.caesar.endpoint.plugins;

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
