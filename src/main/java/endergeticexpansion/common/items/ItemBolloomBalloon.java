package endergeticexpansion.common.items;

import javax.annotation.Nullable;

import endergeticexpansion.common.entities.bolloom.EntityBolloomBalloon;
import endergeticexpansion.common.entities.bolloom.EntityBolloomKnot;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemBolloomBalloon extends Item {
	@Nullable
	private final DyeColor balloonColor;
	
	public ItemBolloomBalloon(Properties properties, @Nullable DyeColor balloonColor) {
		super(properties);
		this.balloonColor = balloonColor;
	}
	
	@Nullable
	public DyeColor getBalloonColor() {
		return this.balloonColor;
	}
	
	@Override
	public ActionResultType onItemUse(ItemUseContext context) {
		BlockPos pos = context.getPos();
		World world = context.getWorld();
		Block block = world.getBlockState(pos).getBlock();
		
		if(block instanceof FenceBlock) {
			if(world.getBlockState(pos.up()).getMaterial().isReplaceable() && world.getBlockState(pos.up()).getBlock() != Blocks.LAVA && world.getBlockState(pos.up(2)).getMaterial().isReplaceable() && world.getBlockState(pos.up(2)).getBlock() != Blocks.LAVA && world.getBlockState(pos.up(3)).getMaterial().isReplaceable() && world.getBlockState(pos.up(3)).getBlock() != Blocks.LAVA) {
				if(!world.isRemote) {
					this.attachToFence(pos, world, context.getItem());
				}
			} else {
				return ActionResultType.FAIL;
			}
			for (Entity entity : world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos))) {
				if(entity instanceof EntityBolloomKnot) {
					if(((EntityBolloomKnot)entity).hasMaxBalloons()) {
						return ActionResultType.FAIL;
					}
				}
	        }
		}
		return ActionResultType.PASS;
	}

	public void attachToFence(BlockPos fencePos, World world, ItemStack stack) {
		for (Entity entity : world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(fencePos))) {
			if(entity instanceof EntityBolloomKnot) {
				if(!((EntityBolloomKnot)entity).hasMaxBalloons()) {
					EntityBolloomKnot setKnot = EntityBolloomKnot.getKnotForPosition(world, fencePos);
					setKnot.addBalloon(this.getBalloonColor());
					stack.shrink(1);
				}
			}
        }
		if(EntityBolloomKnot.getKnotForPosition(world, fencePos) == null) {
			EntityBolloomKnot.createStartingKnot(world, fencePos, this.getBalloonColor());
			stack.shrink(1);
		}
	}

	public static class BalloonDispenseBehavior extends DefaultDispenseItemBehavior {
		
		@SuppressWarnings("deprecation")
		@Override
		protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
			BlockPos blockpos = source.getBlockPos().offset(source.getBlockState().get(DispenserBlock.FACING));
			World world = source.getWorld();
			BlockState state = world.getBlockState(blockpos);
			if(state.getBlock().getMaterial(state).isReplaceable() && stack.getItem() instanceof ItemBolloomBalloon) {
				EntityBolloomBalloon balloon = new EntityBolloomBalloon(world, blockpos);
				balloon.setColor(((ItemBolloomBalloon)stack.getItem()).getBalloonColor());
				world.addEntity(balloon);
				stack.shrink(1);
			} else if(!state.getBlock().getMaterial(state).isReplaceable() && !state.getBlock().isIn(BlockTags.FENCES)) {
				return super.dispenseStack(source, stack);
			} else if(state.getBlock().isIn(BlockTags.FENCES)) {
				if(EntityBolloomKnot.getKnotForPosition(world, blockpos) == null && stack.getItem() instanceof ItemBolloomBalloon) {
					EntityBolloomKnot.createStartingKnot(world, blockpos, ((ItemBolloomBalloon)stack.getItem()).getBalloonColor());
					stack.shrink(1);
					return stack;
				} else {
					for (Entity entity : world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(blockpos))) {
						if(entity instanceof EntityBolloomKnot && stack.getItem() instanceof ItemBolloomBalloon) {
							if(!((EntityBolloomKnot)entity).hasMaxBalloons()) {
								EntityBolloomKnot setKnot = EntityBolloomKnot.getKnotForPosition(world, blockpos);
								setKnot.addBalloon(((ItemBolloomBalloon)stack.getItem()).getBalloonColor());
								stack.shrink(1);
							} else {
								return super.dispenseStack(source, stack);
							}
						}
			        }
				}
			} else {
				return super.dispenseStack(source, stack);
			}
			return stack;
		}
		
	}
	
}
