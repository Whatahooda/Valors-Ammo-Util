# Valor's Ammo Utility
Welcome! This is a Hytale Asset Editor tool to help add different item ammunitions to the game.
In current base Hytale, you have to manually search for a specific item in your inventory with the `modifyInventory` interaction type. Then chain this interaction over and over for each ammunition item you have. Not to mention there is no way to support new weapons or seperate modded weapons.

But no more! This utility mod simplifies that process. It adds new Asset Editor interaction types to build your projectile items and weapons.

## Asset Editor Tools
There are a handful of new interaction types to support more ammunitions. Here's a list
 - [AmmoModifyInventory](#AmmoModifyInventory)
 - [AmmoProjectile](#AmmoProjectile)
 - [AmmoOnHit](#AmmoOnHit-and-AmmoOnMiss)
 - [AmmoOnMiss](#AmmoOnHit-and-AmmoOnMiss)
 - [AmmoRemove](#AmmoRemove)
 - [AmmoInfo](#AmmoInfo)

### AmmoModifyInventory
This is a copy of Hytale's `modifyInventory`, but modified to support looking for more than one item.
There are 4 new entries to customize and process logic for ammo.
1) `TagsToFind` is a array/list of tags to look for in an item. The tag can be located in any tag key.
2) `AmountToRemove`  is the amount of items to remove from that stack.
3) `AmmoInfoVar` is the Interaction Var to access `AmmoInfo` on the item asset. By default, I like to use "Ammo_Info". Then on my item add a interaction var that only includes a `AmmoInfo` interaction type.
4) `UseItemModel` sets whether the created projectile should use the ModelAsset provided in the item's `AmmoInfo`.

Then make sure to add your normal `next` and `failed` interactions to continue the chain. At some point down the chain you'll need to use `AmmoProjectile`.

### AmmoProjectile
Treat `AmmoProjectile` as the exact same as Hytale's `Projectile` interaction type. Call it and provide it with a config to create a projectile. This is needed to process our ammo information and add that information to the spawned projectile.

### AmmoOnHit and AmmoOnMiss
These 2 interactions are what apply any ammo interactions specified in an item's `AmmoInfo`. Insert them somewhere into your projectile config's onHit or onMiss interaction chains.

I like to override `DamageEntityParent` and `Common_Projectile_Miss` and insert onHit and onMiss into them. Though it's probably not best practice to override vanilla assets.

### AmmoRemove
When despawning your ammo projectile, use the `AmmoRemove` interaction. Due to unknown issues, interaction SFX and VFX break when using the default `removeEntity`.

### AmmoInfo
This interaction will always be located in an Interaction Var on your item to be used as ammo. It holds all the information relevant to the created projectile from the ammo item.

It has 3 entries you can fill out, they can also be null.
1) `ModelAssetId` is the string Id of the model asset to use for the projectile.
2) `AmmoOnHitId` is the string Id to a `RootInteraction` containing your on hit interactions for that ammo.
3) `AmmoOnMissId` is the exact same as OnHit, just for on miss.

## Future Plans and Changes
This tool is far from finished, so there's a lot more features I plan on adding.
- **Modder QOL:** Some interaction entries can be made to open a list of assets, rather than manually entering in string Ids
- **Reloadable Support:** Currently reloadable weapons are not supported in any way. At some point I'll get more systems in place to get them working with custom ammo.

## Known Bugs
- **Projectile Models:** Currently projectile models are not being updated with the models specified in item `AmmoInfo`. I am actively looking into fixing this.

## Credits
Valors Ammo Utility was created by [Valor/Whatahooda](https://github.com/Whatahooda)

[GlobalHive](https://github.com/GlobalHive) was a massive help and fixed some bugs I was having trouble with.

Thanks to [Up](https://github.com/UpcraftLP) whom created the template, and [Kaupenjoe](https://github.com/Kaupenjoe) who modified and shared it. 
