package com.bobmowzie.mowziesmobs.server.entity.grottol;

import com.bobmowzie.mowziesmobs.client.particle.ParticleHandler;
import com.bobmowzie.mowziesmobs.server.advancement.AdvancementHandler;
import com.bobmowzie.mowziesmobs.server.ai.EntityAIGrottolFindMinecart;
import com.bobmowzie.mowziesmobs.server.ai.MMAIAvoidEntity;
import com.bobmowzie.mowziesmobs.server.ai.MMEntityMoveHelper;
import com.bobmowzie.mowziesmobs.server.ai.MMPathNavigateGround;
import com.bobmowzie.mowziesmobs.server.ai.animation.AnimationDieAI;
import com.bobmowzie.mowziesmobs.server.ai.animation.AnimationTakeDamage;
import com.bobmowzie.mowziesmobs.server.ai.animation.SimpleAnimationAI;
import com.bobmowzie.mowziesmobs.server.block.ICopiedBlockProperties;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.bobmowzie.mowziesmobs.server.entity.MowzieEntity;
import com.bobmowzie.mowziesmobs.server.entity.MowzieLLibraryEntity;
import com.bobmowzie.mowziesmobs.server.entity.grottol.ai.EntityAIGrottolIdle;
import com.bobmowzie.mowziesmobs.server.item.ItemHandler;
import com.bobmowzie.mowziesmobs.server.loot.LootTableHandler;
import com.bobmowzie.mowziesmobs.server.potion.EffectHandler;
import com.bobmowzie.mowziesmobs.server.sound.MMSounds;
import com.bobmowzie.mowziesmobs.server.tag.TagHandler;
import com.ilexiconn.llibrary.server.animation.Animation;
import com.ilexiconn.llibrary.server.animation.AnimationHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

/**
 * Created by BobMowzie on 7/3/2018.
 */
public class EntityGrottol extends MowzieLLibraryEntity {
    public static final Animation DIE_ANIMATION = Animation.create(73);
    public static final Animation HURT_ANIMATION = Animation.create(10);
    public static final Animation IDLE_ANIMATION = EntityAIGrottolIdle.animation();
    public static final Animation BURROW_ANIMATION = Animation.create(20);
    private static final Animation[] ANIMATIONS = {
            DIE_ANIMATION,
            HURT_ANIMATION,
            IDLE_ANIMATION,
            BURROW_ANIMATION
    };
    public int fleeTime = 0;
    private int timeSinceFlee = 50;
    private int timeSinceMinecart = 0;

    private final BlackPinkRailLine reader = BlackPinkRailLine.create();

    public enum EnumDeathType {
        NORMAL,
        PICKAXE,
        FORTUNE_PICKAXE
    }

    private EnumDeathType death = EnumDeathType.NORMAL;

    private int timeSinceDeflectSound = 0;

    private static final EntityDataAccessor<Boolean> DEEPSLATE = SynchedEntityData.defineId(EntityGrottol.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BLACKPINK = SynchedEntityData.defineId(EntityGrottol.class, EntityDataSerializers.BOOLEAN);

    public EntityGrottol(EntityType<? extends EntityGrottol> type, Level world) {
        super(type, world);
        xpReward = 15;
        setMaxUpStep(1.15F);

        moveControl = new MMEntityMoveHelper(this, 45);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        setPathfindingMalus(BlockPathTypes.DANGER_OTHER, 1);
        setPathfindingMalus(BlockPathTypes.WATER, 3);
        setPathfindingMalus(BlockPathTypes.WATER_BORDER, 3);
        setPathfindingMalus(BlockPathTypes.LAVA, 1);
        setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 1);
        setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 1);
        setPathfindingMalus(BlockPathTypes.DANGER_OTHER, 1);
        setPathfindingMalus(BlockPathTypes.DAMAGE_OTHER, 1);
        goalSelector.addGoal(3, new FloatGoal(this));
        goalSelector.addGoal(4, new RandomStrollGoal(this, 0.3));
        goalSelector.addGoal(1, new EntityAIGrottolFindMinecart(this));
        goalSelector.addGoal(2, new MMAIAvoidEntity<EntityGrottol, Player>(this, Player.class, 16f, 0.5, 0.7) {
            private int fleeCheckCounter = 0;

            @Override
            protected void onSafe() {
                fleeCheckCounter = 0;
            }

            @Override
            protected void onPathNotFound() {
                if (fleeCheckCounter < 4) {
                    fleeCheckCounter++;
                } else if (getAnimation() == NO_ANIMATION) {
                    AnimationHandler.INSTANCE.sendAnimationMessage(entity, EntityGrottol.BURROW_ANIMATION);
                }
            }

            @Override
            public void tick() {
                super.tick();
                entity.fleeTime++;
            }

            @Override
            public void stop() {
                super.stop();
                entity.timeSinceFlee = 0;
                fleeCheckCounter = 0;
            }
        });
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(1, new AnimationTakeDamage<>(this));
        goalSelector.addGoal(1, new AnimationDieAI<>(this));
        goalSelector.addGoal(5, new EntityAIGrottolIdle(this));
        goalSelector.addGoal(2, new SimpleAnimationAI<>(this, BURROW_ANIMATION, false));
    }

    @Override
    public int getMaxFallDistance() {
        return 256;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new MMPathNavigateGround(this, world);
    }

//    @Override
//    public float getWalkTargetValue(BlockPos pos) {
//        return (float) pos.distSqr(this.position(), true);
//    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected float getWaterSlowDown() {
        return 1;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean isEffectiveAi() {
        return super.isEffectiveAi() && !isInMinecart();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MowzieEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    protected ConfigHandler.SpawnConfig getSpawnConfig() {
        return ConfigHandler.COMMON.MOBS.GROTTOL.spawnConfig;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(DEEPSLATE, false);
        getEntityData().define(BLACKPINK, false);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor world, MobSpawnType reason) {
        return getEntitiesNearby(EntityGrottol.class, 20, 20, 20, 20).isEmpty() && super.checkSpawnRules(world, reason);
    }

    @Override
    public boolean skipAttackInteraction(Entity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, player.getMainHandItem()) > 0) {
                if (!level().isClientSide && isAlive()) {
                    spawnAtLocation(ItemHandler.CAPTURED_GROTTOL.get().create(this), 0.0F);
                    BlockState state = Blocks.STONE.defaultBlockState();
                    SoundType sound = state.getBlock().getSoundType(state, level(), this.blockPosition(), entity);
                    level().playSound(
                        null,
                        getX(), getY(), getZ(),
                        sound.getBreakSound(),
                        getSoundSource(),
                        (sound.getVolume() + 1.0F) / 2.0F,
                        sound.getPitch() * 0.8F
                    );
                    if (level() instanceof ServerLevel) {
                        ((ServerLevel) level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                            getX(), getY() + getBbHeight() / 2.0D, getZ(),
                            32,
                            getBbWidth() / 4.0F, getBbHeight() / 4.0F, getBbWidth() / 4.0F,
                            0.05D
                        );
                    }
                    discard() ;
                    if (player instanceof ServerPlayer) AdvancementHandler.GROTTOL_KILL_SILK_TOUCH_TRIGGER.trigger((ServerPlayer) player);
                }
                return true;
            }
        }
        return super.skipAttackInteraction(entity);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity entity = source.getEntity();
        if (entity instanceof Player && !source.is(DamageTypeTags.IS_PROJECTILE)) {
            Player player = (Player) entity;
            if (player.getMainHandItem().isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()) || player.getMainHandItem().is(TagHandler.CAN_HIT_GROTTOL)) {
                if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, player.getMainHandItem()) > 0) {
                    death = EnumDeathType.FORTUNE_PICKAXE;
                    if (player instanceof ServerPlayer) AdvancementHandler.GROTTOL_KILL_FORTUNE_TRIGGER.trigger((ServerPlayer) player);
                } else {
                    death = EnumDeathType.PICKAXE;
                }
                return super.hurt(source, getHealth());
            } else {
                if (timeSinceDeflectSound >= 5) {
                    timeSinceDeflectSound = 0;
                    playSound(MMSounds.ENTITY_GROTTOL_UNDAMAGED.get(), 0.4F, 2.0F);
                }
                return false;
            }
        }
        else if (entity instanceof Mob) {
            return false;
        }
        return super.hurt(source, amount);
    }

    private static BlockState findGroundBelow(Level world, BlockPos pos) {
        while (world.getBlockState(pos).isAir() && pos.getY() > world.getMinBuildHeight()) {
            pos = pos.below();
        }
        return world.getBlockState(pos);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            Entity e = getVehicle();
            if (isMinecart(e)) {
                AbstractMinecart minecart = (AbstractMinecart) e;
                reader.accept(minecart);
                boolean onRail = isBlockRail(level().getBlockState(e.blockPosition()).getBlock()) || isBlockRail(findGroundBelow(level(), e.blockPosition()).getBlock());
                if ((e.onGround() && !level().getBlockState(e.blockPosition().below()).isAir() && !level().getBlockState(e.blockPosition()).isAir())
                        && ((timeSinceMinecart > 3 && e.getDeltaMovement().length() < 0.001) || !onRail)) {
                    minecart.ejectPassengers();
                    timeSinceMinecart = 0;
                }
                else if (onRail) {
                    if (minecart.getDeltaMovement().length() < 0.001) minecart.setDeltaMovement(minecart.getForward().scale(2.7));
                    else minecart.setDeltaMovement(minecart.getDeltaMovement().normalize().scale(2.7));
                    timeSinceMinecart++;
                }
            }
        }

        //Sparkle particles
        if (level().isClientSide && isAlive() && random.nextInt(15) == 0) {
            double x = getX() + 0.5f * (2 * random.nextFloat() - 1f);
            double y = getY() + 0.8f + 0.3f * (2 * random.nextFloat() - 1f);
            double z = getZ() + 0.5f * (2 * random.nextFloat() - 1f);
            if (isBlackPinkInYourArea()) {
                level().addParticle(ParticleTypes.NOTE, x, y, z, random.nextDouble() / 2, 0, 0);
            } else {
                level().addParticle(ParticleHandler.SPARKLE.get(), x, y, z, 0, 0, 0);
            }
        }

        //Footstep Sounds
        float moveX = (float) (getX() - xo);
        float moveZ = (float) (getZ() - zo);
        float speed = Mth.sqrt(moveX * moveX + moveZ * moveZ);
        if (frame % 6 == 0 && speed > 0.05) {
            playSound(MMSounds.ENTITY_GROTTOL_STEP.get(), 1F, 1.8f);
        }

        if (timeSinceFlee < 50) {
            timeSinceFlee++;
        } else {
            fleeTime = 0;
        }

        if (timeSinceDeflectSound < 5) timeSinceDeflectSound++;

        // AI Task
        if (!level().isClientSide && fleeTime >= 55 && getAnimation() == NO_ANIMATION && !isNoAi() && !hasEffect(EffectHandler.FROZEN.get())) {
            BlockState blockBeneath = level().getBlockState(blockPosition().below());
            if (isBlockDiggable(blockBeneath)) {
                AnimationHandler.INSTANCE.sendAnimationMessage(this, BURROW_ANIMATION);
            }
        }
        if (!level().isClientSide && getAnimation() == BURROW_ANIMATION) {
            if (getAnimationTick() % 4 == 3) {
                playSound(MMSounds.ENTITY_GROTTOL_BURROW.get(), 1, 0.8f + random.nextFloat() * 0.4f);
                BlockState blockBeneath = level().getBlockState(blockPosition().below());
                if (isBlockDiggable(blockBeneath)) {
                    Vec3 pos = new Vec3(0.5D, 0.05D, 0.0D).yRot((float) Math.toRadians(-yBodyRot - 90));
                    if (level() instanceof ServerLevel) {
                        ((ServerLevel) level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockBeneath),
                                getX() + pos.x, getY() + pos.y, getZ() + pos.z,
                                8,
                                0.25D, 0.025D, 0.25D,
                                0.1D
                        );
                    }
                }
            }
        }
    }

    @Override
    protected void onAnimationFinish(Animation animation) {
        if (animation == BURROW_ANIMATION) {
            discard() ;
        }
    }

    public static boolean isBlockRail(Block block) {
        return block == Blocks.RAIL || block == Blocks.ACTIVATOR_RAIL || block == Blocks.POWERED_RAIL || block == Blocks.DETECTOR_RAIL;
    }

    private boolean isBlackPinkInYourArea() {
        Entity e = getVehicle();
        if (e instanceof AbstractMinecart) {
            return getBlackpink();
        }
        return false;
    }

    public boolean isInMinecart() {
        return isMinecart(getVehicle());
    }

    /*public boolean hasMinecartBlockDisplay() {
        Entity entity = getRidingEntity();
        return isMinecart(entity) && ((AbstractMinecartEntity) entity).getDisplayTile().getBlock() == BlockHandler.GROTTOL.get();
    }*/

    private static boolean isMinecart(Entity entity) {
        return entity instanceof Minecart;
    }

    @Override
    protected void doPush(Entity entity) {
        if (!isMinecart(entity)) {
            super.doPush(entity);   
        }
    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        /*if (isMinecart(entity)) {
                AbstractMinecartEntity minecart = (AbstractMinecartEntity) entity;
                if (minecart.getDisplayTile().getBlock() != BlockHandler.GROTTOL.get()) {
                    minecart.setDisplayTile(BlockHandler.GROTTOL.get().getDefaultState());
                    minecart.setDisplayTileOffset(minecart.getDefaultDisplayTileOffset());
                }
            }*/
        return super.startRiding(entity, force);
    }

    @Override
    public void stopRiding() {
//        Entity entity = this.getRidingEntity();
        super.stopRiding();
//        if (isMinecart(entity)) {
//            ((AbstractMinecartEntity) entity).setHasDisplayTile(false);
//        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        playSound(MMSounds.ENTITY_GROTTOL_DIE.get(), 1f, 1.3f);
        return null;
    }

    @Override
    public Animation getDeathAnimation() {
        return DIE_ANIMATION;
    }

    @Override
    public Animation getHurtAnimation() {
        return HURT_ANIMATION;
    }

    @Override
    public Animation[] getAnimations() {
        return ANIMATIONS;
    }

    public EnumDeathType getDeathType() {
        return death;
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return LootTableHandler.GROTTOL;
    }

    @Override
    protected ConfigHandler.CombatConfig getCombatConfig() {
        return ConfigHandler.COMMON.MOBS.GROTTOL.combatConfig;
    }

    public boolean isBlockDiggable(BlockState blockState) {
        if (blockState.is(TagHandler.CAN_GROTTOL_DIG)) return true;

        ICopiedBlockProperties properties = (ICopiedBlockProperties) blockState.getBlock().properties;
        Block baseBlock = properties.getBaseBlock();
        if (baseBlock != null) {
            return baseBlock.defaultBlockState().is(TagHandler.CAN_GROTTOL_DIG);
        }

        return false;
    }

    public boolean getBlackpink() {
        return getEntityData().get(BLACKPINK);
    }

    public void setBlackpink(boolean blackpink) {
        getEntityData().set(BLACKPINK, blackpink);
    }

    public boolean getDeepslate() {
        return getEntityData().get(DEEPSLATE);
    }

    public void setDeepslate(boolean deepslate) {
        getEntityData().set(DEEPSLATE, deepslate);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("deepslate", this.getDeepslate());
        compound.putBoolean("blackpink", this.getBlackpink());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setDeepslate(compound.getBoolean("deepslate"));
        setBlackpink(compound.getBoolean("blackpink"));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, SpawnGroupData spawnDataIn, CompoundTag dataTag) {
        if (getY() < 8 && reason != MobSpawnType.MOB_SUMMONED) setDeepslate(true);
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }
}
