# Temperatura · TI3042

Abre el proyecto `temperatura` en Android Studio y ejecuta la configuración `app`.
También puedes instalar el APK de depuración adjunto en un equipo con Android 7.0 o superior.

## Demostración

1. En la primera apertura, toca **Crear cuenta** con un usuario y una contraseña de 15 a 128 caracteres.
2. Ingresa de nuevo la contraseña y toca **Iniciar sesión**. Una contraseña incorrecta conserva la pantalla de acceso.
3. Monitoreo abre en **Modo Arduino**. Mientras falte configurar el enlace, las tarjetas quedan sin lectura y los controles quedan deshabilitados.
4. Toca **Probar simulación**. La etiqueta identifica el origen de las lecturas, que cambian cada dos segundos.
5. Pulsa **Activar** y **Desactivar**. Cada solicitud muestra la espera y actualiza el estado tras la confirmación del simulador.
6. Abre **Historial → Ver lecturas**. Cada fila muestra temperatura, humedad, fecha, hora y origen. La vista consulta las 20 más recientes y SQLite conserva un máximo de 200. Después de cerrar y abrir la app, vuelve a iniciar sesión y consulta el historial.
7. Toca **Cerrar sesión** para volver al acceso. La cuenta sigue disponible tras cerrar la aplicación.

La autenticación es local. El registro guarda un verificador PBKDF2 con sal aleatoria y se cifra con AES-GCM mediante una clave de Android Keystore. Se guarda en el almacenamiento privado excluido de copias de seguridad. El primer usuario que abre una instalación nueva puede crear la cuenta. Para volver al alta inicial, borra los datos de la aplicación en un dispositivo de prueba. La sesión se vuelve a solicitar al recrear la actividad, por ejemplo tras girar el dispositivo.

La evaluación usa un teléfono con simulación local, según el alcance indicado por el docente. Para la simulación no se solicitan permisos de Bluetooth, Wi-Fi ni ubicación. El historial se guarda en la base SQLite privada de la aplicación.

## Conexión con el Arduino Uno

- **Uno R3**. Para conexión inalámbrica requiere un módulo externo. Si el módulo ofrece Bluetooth clásico con un puerto serie, el adaptador Android usaría RFCOMM. Si el módulo ofrece BLE, usaría GATT y las características expuestas por el firmware.
- **Uno R4 WiFi**. Integra Wi-Fi y BLE. Wi-Fi depende de cómo el sketch expone el servicio y de la red disponible. BLE permite una conexión directa si el firmware define las características para mediciones, comandos y confirmaciones.
- Identifica la revisión impresa en la placa y el modelo del módulo, si existe. Comparte el archivo `.ino` y ejemplos de mediciones y respuestas. En BLE harán falta los UUID y las operaciones de cada característica. En Wi-Fi harán falta el servicio, el direccionamiento y el mecanismo de recepción. Si aún no hay confirmación en el sketch, hay que acordarla e implementarla en el firmware y Android.

`DispositivoIoT.kt` define el contrato **interno de Android**. El adaptador futuro debe publicar conexión, mediciones y estado confirmado. Para completar una solicitud debe esperar una respuesta que corresponda al comando y reporte el estado aplicado. La interfaz de usuario aplica un plazo de cinco segundos y deja el último estado confirmado visible si no llega una nueva confirmación. El valor confirmado por el firmware representa lo que este reporte. Verificar el movimiento físico requiere realimentación del actuador.

`MainActivity.kt` conecta las pantallas con la cuenta y el dispositivo. El argumento `dispositivoArduino` queda sin adaptador hasta conocer el enlace y los mensajes. `CuentaLocal.kt` registra y verifica la cuenta.

- [Arduino Uno R3](https://docs.arduino.cc/hardware/uno-rev3/)
- [Manual Arduino Uno R4 WiFi](https://docs.arduino.cc/tutorials/uno-r4-wifi/cheat-sheet)
- [Permisos Bluetooth en Android](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)

## Acceso reforzado

- Cinco fallos bloquean el acceso por un minuto. Las reincidencias aumentan la espera hasta una hora y un acceso correcto restablece el contador. El bloqueo persiste al cerrar la aplicación y, si Android informa un nuevo arranque durante la espera, el plazo comienza de nuevo.
- Las cuentas anteriores se cifran al abrir esta versión y conservan su contraseña. El mínimo de 15 caracteres se aplica al crear una cuenta nueva.
- La sesión se cierra si la aplicación pasa a segundo plano o tras cinco minutos sin interacción. Android bloquea las capturas y grabaciones de sus pantallas.
- Este APK de depuración es para evaluación. La distribución requiere una compilación de lanzamiento firmada. El enlace IoT requiere un canal autenticado y protegido antes de habilitar el control del Arduino físico.

## Pruebas reproducibles

Desde la raíz del proyecto en Windows, ejecuta `gradlew.bat assembleDebug testDebugUnitTest`. Las pruebas unitarias cubren el quinto intento, la escalada y el límite del bloqueo, el cálculo monotónico de la espera y el rechazo de lecturas inválidas. Para la prueba manual del bloqueo, usa una cuenta de demostración, equivócate cinco veces y reabre la aplicación antes de que venza el minuto de espera.
