package endergeticexpansion.core.registry;

import java.util.function.BiFunction;

import endergeticexpansion.common.entities.*;
import endergeticexpansion.common.entities.bolloom.*;
import endergeticexpansion.common.entities.booflo.*;
import endergeticexpansion.core.EndergeticExpansion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class EEEntities {
	public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = new DeferredRegister<>(ForgeRegistries.ENTITIES, EndergeticExpansion.MOD_ID);
	
	/*
	 * Poise Forest
	 */
	public static final RegistryObject<EntityType<EntityPoiseCluster>> POISE_CLUSTER = ENTITY_TYPES.register("poise_cluster", () -> createLivingEntity(EntityPoiseCluster::new, EntityClassification.MISC, "poise_cluster", 1F, 1F));
	public static final RegistryObject<EntityType<EntityBolloomFruit>> BOLLOOM_FRUIT = ENTITY_TYPES.register("bolloom_fruit", () -> createEntity(EntityBolloomFruit::new, EntityBolloomFruit::new, EntityClassification.MISC, "bolloom_fruit", 0.5F, 0.5F));
	public static final RegistryObject<EntityType<EntityBoofBlock>> BOOF_BLOCK = ENTITY_TYPES.register("boof_block", () -> createLivingEntity(EntityBoofBlock::new, EntityClassification.MISC, "boof_block", 1.6F, 1.6F));
	public static final RegistryObject<EntityType<EntityPuffBug>> PUFF_BUG = ENTITY_TYPES.register("puff_bug", () -> createLivingEntity(EntityPuffBug::new, EntityClassification.CREATURE, "puff_bug", 0.3F, 1.0F));
	public static final RegistryObject<EntityType<EntityEndergeticBoat>> BOAT = ENTITY_TYPES.register("boat", () -> createEntity(EntityEndergeticBoat::new, EntityEndergeticBoat::new, EntityClassification.MISC, "boat", 1.375F, 0.5625F));
	public static final RegistryObject<EntityType<EntityBolloomBalloon>> BOLLOOM_BALLOON = ENTITY_TYPES.register("bolloom_balloon", () -> createEntity(EntityBolloomBalloon::new, EntityBolloomBalloon::new, EntityClassification.MISC, "bolloom_balloon", 0.5F, 0.5F));
	public static final RegistryObject<EntityType<EntityBolloomKnot>> BOLLOOM_KNOT = ENTITY_TYPES.register("bolloom_knot", () -> createEntity(EntityBolloomKnot::new, EntityBolloomKnot::new, EntityClassification.MISC, "bolloom_knot", 0.375F, 0.19F));
	public static final RegistryObject<EntityType<EntityBoofloBaby>> BOOFLO_BABY = ENTITY_TYPES.register("booflo_baby", () -> createLivingEntity(EntityBoofloBaby::new, EntityClassification.CREATURE, "booflo_baby", 0.375F, 0.325F));
	public static final RegistryObject<EntityType<EntityBoofloAdolescent>> BOOFLO_ADOLESCENT = ENTITY_TYPES.register("booflo_adolescent", () -> createLivingEntity(EntityBoofloAdolescent::new, EntityClassification.CREATURE, "booflo_adolescent", 0.8F, 0.7F));
	public static final RegistryObject<EntityType<EntityBooflo>> BOOFLO = ENTITY_TYPES.register("booflo", () -> createLivingEntity(EntityBooflo::new, EntityClassification.CREATURE, "booflo", 1.3F, 1.3F));
	
	private static <T extends LivingEntity> EntityType<T> createLivingEntity(EntityType.IFactory<T> factory, EntityClassification entityClassification, String name, float width, float height){
		ResourceLocation location = new ResourceLocation(EndergeticExpansion.MOD_ID, name);
		EntityType<T> entity = EntityType.Builder.create(factory, entityClassification)
			.size(width, height)
			.setTrackingRange(64)
			.setShouldReceiveVelocityUpdates(true)
			.setUpdateInterval(3)
			.build(location.toString()
		);
		return entity;
	}
	
	private static <T extends Entity> EntityType<T> createEntity(EntityType.IFactory<T> factory, BiFunction<FMLPlayMessages.SpawnEntity, World, T> clientFactory, EntityClassification entityClassification, String name, float width, float height) {
		ResourceLocation location = new ResourceLocation(EndergeticExpansion.MOD_ID, name);
		EntityType<T> entity = EntityType.Builder.create(factory, entityClassification)
			.size(width, height)
			.setTrackingRange(64)
			.setShouldReceiveVelocityUpdates(true)
			.setUpdateInterval(3)
			.setCustomClientFactory(clientFactory)
			.build(location.toString()
		);
		return entity;
	}
}