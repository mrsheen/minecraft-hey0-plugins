mCurrency plugin for hey0 107+
by The Maniac
v1.00

------------------------------------------------------------------------------------

Changelog:

10/6/2010

- Commands will now be removed from /help when plugin is disabled
- Changed /spawnmoney to /deposit, /delmoney to /debit, and /givemoney to /pay
- Removed some extraneous debugging messages
- Added money over time
- Added configurable money name
- Lots of commenting and code cleanup

------------------------------------------------------------------------------------

Installation:

1. Copoy mCurrency.jar into <minecraft_mod_dir>/plugins
2. Add "mCurrency" to the plugins line in server.properties
3. Edit mCurrency.properties after first run to configure

------------------------------------------------------------------------------------

Commands:

/deposit [amount] <player> - (admin) Adds [amount] money to <player>'s balance
/debit [amount] <player> - (admin) Subtracts [amount] money from <player>'s balance
/pay [amount] [player] - Gives [amount] money to [player].
/money - Prints current player balance.
/rank - Displays player's rank
/top5 - Displays top 5 richest players

------------------------------------------------------------------------------------

Configuration:

dataFile 		Path to mCurrency data file
startingBalance		Balance new players will start with
moneyName		Name of money, defaults to "coin"
moneyTickInterval	Time between money ticks. Set to 0 to disable money over time
moneyTick		Amount of money to give players after each moneyTickInterval

------------------------------------------------------------------------------------

Post comments, bugs, etc. @ http://forum.hey0.net/viewtopic.php?id=143