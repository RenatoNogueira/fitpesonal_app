# Regras adicionais de R8 (Room/Hilt já trazem as suas via consumer rules)

# SQLCipher: classes acessadas por JNI não podem ser renomeadas/removidas
-keep,includedescriptorclasses class net.zetetic.database.** { *; }
-keep,includedescriptorclasses interface net.zetetic.database.** { *; }
