# Regras adicionais de R8 (Room/Hilt já trazem as suas via consumer rules)

# SQLCipher: classes acessadas por JNI não podem ser renomeadas/removidas
-keep,includedescriptorclasses class net.zetetic.database.** { *; }
-keep,includedescriptorclasses interface net.zetetic.database.** { *; }

# Relatórios de falha legíveis: mantém arquivo/linha e envia o mapping.txt ao Play Console a cada versão.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Anotações e assinaturas genéricas usadas por Room, Hilt e pelo Kotlin
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
