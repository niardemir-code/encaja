# Reglas R8 de Encaja.
# Hilt, Room, Firebase y Compose aportan sus propias reglas (consumer rules).

# Conservar información útil en los informes de fallos
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature
-renamesourcefileattribute SourceFile

# Modelos que Firestore (re)construye por reflexión, si los hubiera
-keepclassmembers class com.encaja.app.** { @com.google.firebase.firestore.PropertyName <fields>; }
