package endergeticexpansion.common.entities.booflo;

import java.util.UUID;

import javax.annotation.Nullable;

import endergeticexpansion.api.endimator.EndimatedEntity;
import endergeticexpansion.api.endimator.Endimation;
import endergeticexpansion.api.entity.util.EntityItemStackHelper;
import endergeticexpansion.api.util.NetworkUtil;
import endergeticexpansion.common.entities.booflo.ai.BoofloBoofGoal;
import endergeticexpansion.common.entities.booflo.ai.BoofloBreedGoal;
import endergeticexpansion.common.entities.booflo.ai.BoofloFaceRandomGoal;
import endergeticexpansion.common.entities.booflo.ai.BoofloGiveBirthGoal;
import endergeticexpansion.common.entities.booflo.ai.BoofloGroundHopGoal;
import endergeticexpansion.common.entities.booflo.ai.BoofloSwimGoal;
import endergeticexpansion.common.entities.booflo.ai.BoofloSwimmingGoal;
import endergeticexpansion.core.registry.EEBlocks;
import endergeticexpansion.core.registry.EEEntities;
import endergeticexpansion.core.registry.EEItems;
import endergeticexpansion.core.registry.EESounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.controller.LookController;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class EntityBooflo extends EndimatedEntity {
	private static final DataParameter<Boolean> MOVING_IN_AIR = EntityDataManager.createKey(EntityBooflo.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> BOOFED = EntityDataManager.createKey(EntityBooflo.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> PREGNANT = EntityDataManager.createKey(EntityBooflo.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Integer> LOVE_TICKS = EntityDataManager.createKey(EntityBooflo.class, DataSerializers.VARINT);
	private static final DataParameter<Float> BIRTH_YAW = EntityDataManager.createKey(EntityBooflo.class, DataSerializers.FLOAT);
	public static final Endimation CROAK = new Endimation(55);
	public static final Endimation HOP = new Endimation(25);
	public static final Endimation HURT = new Endimation(15);
	public static final Endimation BIRTH = new Endimation(140);
	public static final Endimation INFLATE = new Endimation(10);
	public static final Endimation SWIM = new Endimation(20);
	private static final EntitySize BOOFED_SIZE = EntitySize.fixed(2.0F, 1.5F);
	public int hopDelay;
	private UUID playerInLove;

	public EntityBooflo(EntityType<? extends EntityBooflo> type, World worldIn) {
		super(type, worldIn);
		this.moveController = new GroundMoveHelperController(this);
		this.hopDelay = this.getDefaultGroundHopDelay();
	}
	
	@Override
	protected void registerAttributes() {
		super.registerAttributes();
		this.getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0D);
		this.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(1.05D);
		this.getAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(2.0D);
		this.getAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(22.0D);
	}
	
	@Override
	protected void registerData() {
		super.registerData();
		this.getDataManager().register(MOVING_IN_AIR, false);
		this.getDataManager().register(BOOFED, false);
		this.getDataManager().register(PREGNANT, false);
		this.getDataManager().register(LOVE_TICKS, 0);
		this.getDataManager().register(BIRTH_YAW, 0.0F);
	}
	
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new BoofloGiveBirthGoal(this));
		this.goalSelector.addGoal(1, new BoofloBreedGoal(this));
		this.goalSelector.addGoal(2, new BoofloBoofGoal(this));
		this.goalSelector.addGoal(3, new BoofloSwimmingGoal(this));
		this.goalSelector.addGoal(4, new BoofloSwimGoal(this, 1.0F, 15));
		this.goalSelector.addGoal(5, new BoofloFaceRandomGoal(this));
		this.goalSelector.addGoal(6, new BoofloGroundHopGoal(this));
	}
	
	@Override
	public void tick() {
		super.tick();
		
		if(this.isAnimationPlaying(SWIM) && this.getAnimationTick() == 2) {
			float xMotion = -MathHelper.sin(this.rotationYaw * ((float) Math.PI / 180F)) * MathHelper.cos(this.rotationPitch * ((float) Math.PI / 180F));
			float yMotion = -MathHelper.sin(this.rotationPitch * ((float) Math.PI / 180F));
			float zMotion = MathHelper.cos(this.rotationYaw * ((float) Math.PI / 180F)) * MathHelper.cos(this.rotationPitch * ((float) Math.PI / 180F));
			
			Vec3d motion = new Vec3d(xMotion, yMotion, zMotion).normalize().scale(0.5D);
			
			this.addVelocity(motion.x, motion.y, motion.z);
		}
		
		if(this.isAnimationPlaying(INFLATE) && this.getAnimationTick() == 2) {
			this.boof();
		}
		
		if(this.isInWater() && !this.isBoofed()) {
			this.setBoofed(true);
		}
		
		if(this.isInWater() && this.isBoofed() && this.getRNG().nextFloat() < 0.7F) {
			this.addVelocity(0.0F, 0.05F, 0.0F);
		}
		
		if(this.onGround && this.isBoofed()) {
			this.setBoofed(false);
		}
	}
	
	@Override
	public void livingTick() {
		super.livingTick();
		
		if(this.hopDelay > 0) {
			this.hopDelay--;
		}
		
		if(this.isPregnant()) {
			this.resetInLove();
		}
		
		if(this.getInLoveTicks() > 0) {
			this.setInLove(this.getInLoveTicks() - 1);
			if(this.getInLoveTicks() % 10 == 0) {
				double d0 = this.rand.nextGaussian() * 0.02D;
				double d1 = this.rand.nextGaussian() * 0.02D;
				double d2 = this.rand.nextGaussian() * 0.02D;
				this.world.addParticle(ParticleTypes.HEART, this.posX + (this.rand.nextFloat() * this.getWidth() * 2.0F) - this.getWidth(), this.posY + 0.5D + (this.rand.nextFloat() * this.getHeight()), this.posZ + (this.rand.nextFloat() * this.getWidth() * 2.0F) - this.getWidth(), d0, d1, d2);
			}
		}
		
		if(!this.isWorldRemote() && this.isAlive() && this.onGround && !this.isBoofed() && this.rand.nextInt(1000) < this.livingSoundTime++ && this.isAnimationPlaying(BLANK_ANIMATION)) {
			this.livingSoundTime = -this.getTalkInterval();
			NetworkUtil.setPlayingAnimationMessage(this, CROAK);
		}
		
		if(this.isAnimationPlaying(CROAK) && this.getAnimationTick() == 5 && !this.isWorldRemote()) {
			this.playSound(this.getAmbientSound(), this.getSoundVolume(), this.getSoundPitch());
		}
	}
	
	@Override
	public void writeAdditional(CompoundNBT compound) {
		super.writeAdditional(compound);
		compound.putBoolean("IsMovingInAir", this.isMovingInAir());
		compound.putBoolean("IsBoofed", this.isBoofed());
		compound.putBoolean("IsPregnant", this.isPregnant());
		compound.putInt("InLove", this.getInLoveTicks());
		compound.putFloat("BirthYaw", this.getBirthYaw());
		if(this.playerInLove != null) {
			compound.putUniqueId("LoveCause", this.playerInLove);
		}
	}
	
	@Override
	public void readAdditional(CompoundNBT compound) {
		super.readAdditional(compound);
		this.setMovingInAir(compound.getBoolean("IsMovingInAir"));
		this.setBoofed(compound.getBoolean("IsBoofed"));
		this.setPregnant(compound.getBoolean("IsPregnant"));
		this.setInLove(compound.getInt("InLove"));
		this.setBirthYaw(compound.getFloat("BirthYaw"));
		this.playerInLove = compound.hasUniqueId("LoveCause") ? compound.getUniqueId("LoveCause") : null;
	}
	
	@Override
	public void notifyDataManagerChange(DataParameter<?> key) {
		if(BOOFED.equals(key)) {
			this.recalculateSize();
			if(this.isBoofed()) {
				this.navigator = new FlyingPathNavigator(this, this.world) {
					
					@Override
					public boolean canEntityStandOnPos(BlockPos pos) {
						return this.world.isAirBlock(pos);
					}
					
				};
				this.moveController = new FlyingMoveContoller(this);
				this.lookController = new FlyingLookController(this, 10);
				
				if(!this.isWorldRemote() && this.ticksExisted > 5) {
					this.playSound(this.getInflateSound(), this.getSoundVolume(), this.getSoundPitch());
				}
			} else {
				this.navigator = this.createNavigator(this.world);
				this.moveController = new GroundMoveHelperController(this);
				this.lookController = new LookController(this);
				
				if(!this.isWorldRemote() && this.ticksExisted > 5) {
					this.playSound(this.getDeflateSound(), this.getSoundVolume(), this.getSoundPitch());
				}
			}
		}
	}
	
	@Override
	public void travel(Vec3d vec3d) {
		if(this.isServerWorld() && this.isBoofed()) {
			this.moveRelative(0.0F, vec3d);
			this.move(MoverType.SELF, this.getMotion());
			this.setMotion(this.getMotion().scale(0.9D));
			if(!this.isMovingInAir()) {
				this.setMotion(this.getMotion().subtract(0, 0.01D, 0));
			}
		} else {
			super.travel(vec3d);
		}
	}
	
	public boolean isMovingInAir() {
		return this.getDataManager().get(MOVING_IN_AIR);
	}

	public void setMovingInAir(boolean moving) {
		this.getDataManager().set(MOVING_IN_AIR, moving);
	}
	
	public boolean isBoofed() {
		return this.dataManager.get(BOOFED);
	}
	
	public void setBoofed(boolean boofed) {
		this.dataManager.set(BOOFED, boofed);
	}
	
	public boolean isPregnant() {
		return this.dataManager.get(PREGNANT);
	}
	
	public void setPregnant(boolean pregnant) {
		this.dataManager.set(PREGNANT, pregnant);
	}
	
	public float getBirthYaw() {
		return this.dataManager.get(BIRTH_YAW);
	}
	
	public void setBirthYaw(float yaw) {
		this.dataManager.set(BIRTH_YAW, yaw);
	}
	
	public void setInLove(@Nullable PlayerEntity player) {
		this.setInLove(600);
		if(player != null) {
			this.playerInLove = player.getUniqueID();
		}

		this.world.setEntityState(this, (byte) 18);
	}

	public void setInLove(int ticks) {
		this.dataManager.set(LOVE_TICKS, ticks);
	}
	
	public int getInLoveTicks() {
		return this.dataManager.get(LOVE_TICKS);
	}
	
	public boolean canBreed() {
		return this.getInLoveTicks() <= 0 && !this.isPregnant();
	}
	
	public boolean isInLove() {
		if(this.isPregnant()) {
			return false;
		}
		return this.getInLoveTicks() > 0;
	}
	
	public void resetInLove() {
		this.setInLove(0);
	}
	
	@Nullable
	public ServerPlayerEntity getLoveCause() {
		if(this.playerInLove == null) {
			return null;
		} else {
			PlayerEntity playerentity = this.world.getPlayerByUuid(this.playerInLove);
			return playerentity instanceof ServerPlayerEntity ? (ServerPlayerEntity) playerentity : null;
		}
	}
	
	public boolean canMateWith(EntityBooflo possibleMate) {
		if(possibleMate == this) {
			return false;
		} else {
			return this.isInLove() && possibleMate.isInLove();
		}
	}
	
	public int getDefaultGroundHopDelay() {
		return this.isInLove() ? this.rand.nextInt(10) + 25 : this.rand.nextInt(40) + 80;
	}
	
	public void boof() {
		if(!this.isWorldRemote()) {
			this.addVelocity(-MathHelper.sin((float) (this.rotationYaw * Math.PI / 180.0F)) * (4F * (rand.nextFloat() + 0.1F)) * 0.1F, 1.3F, MathHelper.cos((float) (this.rotationYaw * Math.PI / 180.0F)) * (4F * (rand.nextFloat() + 0.1F)) * 0.1F);
		}
		for(Entity entity : this.world.getEntitiesWithinAABB(Entity.class, this.getBoundingBox().grow(3.5F))) {
			if(entity != this && (entity instanceof ItemEntity || entity instanceof LivingEntity)) {
				entity.addVelocity(-MathHelper.sin((float) (entity.rotationYaw * Math.PI / 180.0F)) * (8F * (rand.nextFloat() + 0.1F)) * 0.1F, this.rand.nextFloat() * 0.45D + 0.35D, MathHelper.cos((float) (entity.rotationYaw * Math.PI / 180.0F)) * (8F * (rand.nextFloat() + 0.1F)) * 0.1F);
			}
		}
	}
	
	@Override
	protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
		return this.isBoofed() ? 1.2F : 0.9F;
	}
	
	@Override
	public int getTalkInterval() {
		return 120;
	}
	
	@Override
	public Endimation[] getAnimations() {
		return new Endimation[] {
			CROAK,
			HOP,
			HURT,
			BIRTH,
			INFLATE,
			SWIM
		};
	}
	
	@Override
	public Endimation getHurtAnimation() {
		return HURT;
	}
	
	@Override
	protected void jump() {
		Vec3d vec3d = this.getMotion();
		this.setMotion(vec3d.x, 0.55D, vec3d.z);
		this.isAirBorne = true;
	}
	
	public void setMovementSpeed(double newSpeed) {
		this.getNavigator().setSpeed(newSpeed);
		this.moveController.setMoveTo(this.moveController.getX(), this.moveController.getY(), this.moveController.getZ(), newSpeed);
	}
	
	@Override
	protected void collideWithEntity(Entity entity) {
		if(entity instanceof EntityBoofloBaby && (((EntityBoofloBaby) (entity)).isBeingBorn() || ((EntityBoofloBaby) (entity)).getMotherNoClipTicks() > 0)) return;
		
		super.collideWithEntity(entity);
	}
	
	@Override
	protected boolean processInteract(PlayerEntity player, Hand hand) {
		ItemStack itemstack = player.getHeldItem(hand);
		Item item = itemstack.getItem();
		
		if(item instanceof SpawnEggItem && ((SpawnEggItem)item).hasType(itemstack.getTag(), this.getType())) {
			if(!this.world.isRemote) {
				EntityBoofloBaby baby = EEEntities.BOOFLO_BABY.get().create(this.world);
				baby.setGrowingAge(-24000);
				baby.setLocationAndAngles(this.posX, this.posY, this.posZ, 0.0F, 0.0F);
				this.world.addEntity(baby);
				if(itemstack.hasDisplayName()) {
					baby.setCustomName(itemstack.getDisplayName());
				}
				
				EntityItemStackHelper.consumeItemFromStack(player, itemstack);
			}
			return true;
		} else if(item == EEBlocks.POISE_CLUSTER.asItem() && this.canBreed()) {
			EntityItemStackHelper.consumeItemFromStack(player, itemstack);
			this.setInLove(player);
		}
		return false;
	}
	
	@Override
	public void updatePassenger(Entity passenger) {
		if(this.isPassenger(passenger)) {
			int passengerIndex = this.getPassengers().indexOf(passenger);
			
			double xOffset = passengerIndex == 0 ? 0.25F : -0.25F;
			double zOffset = passengerIndex == 0 ? 0.0F : passengerIndex == 1 ? -0.25F : 0.25F;
			Vec3d ridingOffset = (new Vec3d(xOffset, 0.0D, zOffset)).rotateYaw(-this.getBirthYaw() * ((float) Math.PI / 180F) - ((float) Math.PI / 2F));
			
			passenger.setPosition(this.posX + ridingOffset.x, this.posY + 0.9F, this.posZ + ridingOffset.z);
		}
	}
	
	@Override
	protected boolean canFitPassenger(Entity passenger) {
		int limit = this.isPregnant() ? 3 : 1;
		return this.getPassengers().size() < limit;
	}
	
	@Override
	public int getHorizontalFaceSpeed() {
		return 1;
	}
	
	@Override
	public int getMaxSpawnedInChunk() {
		return 2;
	}
	
	@Override
	public EntitySize getSize(Pose poseIn) {
		return this.isBoofed() ? BOOFED_SIZE : super.getSize(poseIn);
	}
	
	@Override
	public void fall(float distance, float damageMultiplier) {}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void handleStatusUpdate(byte id) {
		if(id == 18) {
			for(int i = 0; i < 7; ++i) {
				double d0 = this.rand.nextGaussian() * 0.02D;
				double d1 = this.rand.nextGaussian() * 0.02D;
				double d2 = this.rand.nextGaussian() * 0.02D;
				this.world.addParticle(ParticleTypes.HEART, this.posX + (double)(this.rand.nextFloat() * this.getWidth() * 2.0F) - (double)this.getWidth(), this.posY + 0.5D + (double)(this.rand.nextFloat() * this.getHeight()), this.posZ + (double)(this.rand.nextFloat() * this.getWidth() * 2.0F) - (double)this.getWidth(), d0, d1, d2);
			}
		} else {
			super.handleStatusUpdate(id);
		}
	}
	
	/*
	 * Overridden to do nothing; gets remade in this class 
	 * @see EntityBooflo#livingTick
	 */
	@Override
	public void playAmbientSound() {}
	
	protected SoundEvent getInflateSound() {
		return EESounds.BOOFLO_INFLATE.get();
	}
	
	protected SoundEvent getDeflateSound() {
		return EESounds.BOOFLO_DEFLATE.get();
	}
	
	@Override
	protected SoundEvent getAmbientSound() {
		return EESounds.BOOFLO_CROAK.get();
	}
	
	@Override
	protected SoundEvent getDeathSound() {
		return EESounds.BOOFLO_DEATH.get();
	}
	
	@Override
	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return EESounds.BOOFLO_HURT.get();
	}
	
	@Override
	public ItemStack getPickedResult(RayTraceResult target) {
		return new ItemStack(EEItems.BOOFLO_SPAWN_EGG.get());
	}
	
	public static class GroundMoveHelperController extends MovementController {
		private final EntityBooflo booflo;
		private float yRot;
		public int hopDelay;
		public boolean isAggressive;

		public GroundMoveHelperController(EntityBooflo booflo) {
			super(booflo);
			this.booflo = booflo;
			this.yRot = (float) (180.0F * booflo.rotationYaw / Math.PI);
		}

		public void setDirection(float yRot, boolean aggressive) {
			this.yRot = yRot;
			this.isAggressive = aggressive;
		}

		public void setSpeed(double speed) {
			this.speed = speed;
			this.action = MovementController.Action.MOVE_TO;
		}

		public void tick() {
			this.mob.rotationYaw = this.limitAngle(this.mob.rotationYaw, this.yRot, 90.0F);
			this.mob.rotationYawHead = this.mob.rotationYaw;
			this.mob.renderYawOffset = this.mob.rotationYaw;
			
			if(this.action != MovementController.Action.MOVE_TO) {
				this.mob.setMoveForward(0.0F);
			} else {
				this.action = MovementController.Action.WAIT;
				if(this.mob.onGround) {
					this.mob.setAIMoveSpeed((float) (this.speed * this.mob.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getValue()));
					if(this.booflo.hopDelay == 0 && this.booflo.isAnimationPlaying(HOP) && this.booflo.getAnimationTick() == 10) {
						this.booflo.getJumpController().setJumping();
						
						this.booflo.hopDelay = this.booflo.getDefaultGroundHopDelay();
					} else {
						this.booflo.moveStrafing = 0.0F;
						this.booflo.moveForward = 0.0F;
						this.mob.setAIMoveSpeed(0.0F);
					}
				} else {
					this.mob.setAIMoveSpeed((float)(this.speed * this.mob.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getValue()));
				}
			}
		}
	}
	
	public class FlyingMoveContoller extends MovementController {
		private final EntityBooflo booflo;

		public FlyingMoveContoller(EntityBooflo booflo) {
			super(booflo);
			this.booflo = booflo;
		}

		public void tick() {
			if(this.action == MovementController.Action.MOVE_TO && !this.booflo.getNavigator().noPath()) {
				Vec3d vec3d = new Vec3d(this.posX - this.booflo.posX, this.posY - this.booflo.posY, this.posZ - this.booflo.posZ);
				double d0 = vec3d.length();
				double d1 = vec3d.y / d0;
				float f = (float) (MathHelper.atan2(vec3d.z, vec3d.x) * (double) (180F / (float) Math.PI)) - 90F;
				
				this.booflo.rotationYaw = this.limitAngle(this.booflo.rotationYaw, f, 10.0F);
				this.booflo.renderYawOffset = this.booflo.rotationYaw;
				this.booflo.rotationYawHead = this.booflo.rotationYaw;
				
				float f1 = (float)(this.speed * this.booflo.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getValue());
				float f2 = MathHelper.lerp(0.125F, this.booflo.getAIMoveSpeed(), f1);
				
				this.booflo.setAIMoveSpeed(f2);
				
				double d3 = Math.cos((double)(this.booflo.rotationYaw * ((float)Math.PI / 180F)));
				double d4 = Math.sin((double)(this.booflo.rotationYaw * ((float)Math.PI / 180F)));
				double d5 = Math.sin((double)(this.booflo.ticksExisted + this.booflo.getEntityId()) * 0.75D) * 0.05D;
				
				if (!this.booflo.isInWater()) {
					float f3 = -((float)(MathHelper.atan2(vec3d.y, (double)MathHelper.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z)) * (double)(180F / (float)Math.PI)));
					f3 = MathHelper.clamp(MathHelper.wrapDegrees(f3), -85.0F, 85.0F);
					this.booflo.rotationPitch = this.limitAngle(this.booflo.rotationPitch, f3, 5.0F);
				}
				
				this.booflo.setMotion(this.booflo.getMotion().add(0, d5 * (d4 + d3) * 0.25D + (double)f2 * d1 * 0.02D, 0));
				
				this.booflo.setMovingInAir(true);
			} else {
				this.booflo.setAIMoveSpeed(0F);
				this.booflo.setMovingInAir(false);
			}
		}
	}
	
	class FlyingLookController extends LookController {
		private final int angleLimit;

		public FlyingLookController(EntityBooflo booflo, int angleLimit) {
			super(booflo);
			this.angleLimit = angleLimit;
		}

		public void tick() {
			if(this.isLooking) {
				this.isLooking = false;
				this.mob.rotationYawHead = this.func_220675_a(this.mob.rotationYawHead, this.func_220678_h() + 20.0F, this.deltaLookYaw);
				this.mob.rotationPitch = this.func_220675_a(this.mob.rotationPitch, this.func_220677_g() + 10.0F, this.deltaLookPitch);
			} else {
				if(this.mob.getNavigator().noPath()) {
					this.mob.rotationPitch = this.func_220675_a(this.mob.rotationPitch, 0.0F, 5.0F);
				}
				this.mob.rotationYawHead = this.func_220675_a(this.mob.rotationYawHead, this.mob.renderYawOffset, this.deltaLookYaw);
			}

			float wrappedDegrees = MathHelper.wrapDegrees(this.mob.rotationYawHead - this.mob.renderYawOffset);
			if(wrappedDegrees < (float)(-this.angleLimit)) {
				this.mob.renderYawOffset -= 4.0F;
			} else if (wrappedDegrees > (float)this.angleLimit) {
				this.mob.renderYawOffset += 4.0F;
			}
		}
	}
}