# Pandemonium (Fabric 26.3)

Enfermedades, fobias y discapacidades absurdas que te caen por lo que haces.
**Solo hace falta en el servidor / quien aloje la partida.** Los amigos entran sin instalar nada y juegan igual.
Si un jugador SÍ lo instala en su cliente, además ve el panel de condiciones y los efectos de pantalla.

## Conseguir el .jar (con GitHub)

1. Crea un repositorio nuevo en https://github.com/new
2. Sube el contenido de esta carpeta (arrastra `src` y los 3 archivos `.gradle`/`.properties`).
3. Crea a mano `.github/workflows/build.yml` (Add file -> Create new file) y pega el contenido del archivo `build.yml` de este zip.
4. Pestaña **Actions**: espera al tick verde y descarga `pandemonium-jar` en **Artifacts**.

## Usarlo

Pon el .jar en `mods` junto a **Fabric API** (para 26.3), en la instancia que aloja la partida (y en las de los amigos que quieran los efectos visuales).

## Cómo te caen las cosas

- Te pega un zombi / husk / ahogado: infección zombi (se contagia entre jugadores cercanos)
- Baya silvestre o cactus: tétanos
- Pasar rato entre flores: alergia al polen
- Abrir el mismo cofre 3 veces seguidas: TOC
- Caída fuerte: huesos frágiles / miedo a las alturas
- Casi ahogarte: asma. Rayo: tics. Explosión: demencia. Picadura de abeja: TDAH
- Muchas noches sin dormir: narcolepsia. Al azar: hipo
- Comer algo que te da hambre (carne podrida...): intoxicación
- Mirarte `/pandemonium list` 3 veces en 5 minutos: hipocondría

## Comandos

- `/pandemonium list` tus condiciones
- `/pandemonium catalog` todas las que existen
- `/pandemonium give <jugadores> <id|random>` (op)
- `/pandemonium remove <jugadores> <id|all>` (op)

## Efectos visuales (solo en clientes con el mod)

Panel de condiciones arriba a la izquierda, viñetas verdes con la infección y la intoxicación, destello al estornudar,
párpados que se cierran con la narcolepsia, glitch en los tics, viñeta negra con el vértigo, cuenta atrás del TOC.

## Novedades (versión estilizada)
- **Infección zombi por etapas** (hasta que mueras): 1 fiebre → 2 brazos de zombi hacia delante (lo ven todos) → 3 el sol te quema (casco = protección) → 4 impulsos de pegar a jugadores/aldeanos cercanos. Contagia a quien esté pegado.
- **Tétanos** por fases y permanente hasta la muerte.
- **Hipocondría**: te salen enfermedades falsas en el panel.
- **Asma**: ataques de 20 s (no puedes correr, toses con partículas).
- **Vómitos**: chorro de partículas verdes desde la cara hacia donde miras.
- **Artrosis**: torso doblado ~15° y cámara inclinada igual (caídas fuertes o frío extremo).
- **Ceguera**: casi todo negro; ruidos y movimientos cercanos aparecen como destellos (explosiones).
- Inclinar cámara y poses usan mixins opcionales (si fallan, el mod sigue funcionando).
- **Gripe** (10 min): tos con partículas; si toses a alguien en la cara sin mascarilla se la pegas, y por cercanía constante (<2 bloques). Se pilla con frío/lluvia o de otros. "Mascarilla" = llevar algo en la cabeza (casco, calabaza...). El zombi también tose desde la etapa 2 y se contagia igual (además de por golpes y cercanía).

## Mascarillas y más enfermedades
- **Mascarilla**: en mesa de crafteo, en fila: hilo + lana blanca + hilo → 2 mascarillas (se equipan en la cabeza). **Filtro**: papel + carbón vegetal → 2 filtros. Con la mascarilla puesta y filtros en el inventario, se usan solos (10 min cada uno). Contagio: sin nada 100%, casco 80%, mascarilla 45%, con filtro 12% (cuenta el que contagia y el que recibe). `/pandemonium recipes` lo explica en el juego.
- Funciona sin mod en el cliente (todo son datos vanilla); la mascarilla se ve como papel.
- Nuevas: congelación, golpe de calor (Nether), rabia (3 fases, lobos/zorros, contagia por golpes), varicela (granitos en pantalla), conmoción, intolerancia a la lactosa, migraña, temblores.
- Animaciones: el que tose agacha la cabeza con la mano en la boca, el que vomita se encorva, el que tirita se sacude (lo ven todos); panel con entrada deslizante y destello.
- **Tinnitus**: pitido agudo permanente que a ratos sube (explosiones 30%).
- **Texturas**: mascarilla y filtro tienen icono propio y la mascarilla se ve puesta en la cara. Es un recurso del mod: quien tenga el mod en el cliente lo ve; sin mod verá papel (y sin la mascarilla visible en la cara).

## Tanda 4 (más condiciones y dinamismo)
- Nuevas: insomnio (fantasmas/mucho sin dormir; te despiertas de la cama), daltonismo (mundo apagado), esguince (caídas; no corres), cinetosis (barcos/caballos), anemia (hambre; desmayos), trastorno de pánico (monstruos cerca), aracnofobia, claustrofobia (en sitios cerrados), sangrado (heridas; gotas de sangre), alergia a los gatos y epilepsia (convulsiones con estrobo).
- Auras de partículas que ven todos: esporas verdes (zombi), notas (tinnitus), estrellas girando (conmoción), chispas (migraña), motas rojas (varicela).
- Panel: las condiciones graves (fase/etapa 3-4) parpadean.

## Moodles
- Las condiciones ahora salen como iconos (moodles) a la derecha, con barritas de gravedad y parpadeo si es grave.
- Abre el chat (T) o el inventario (E) y pasa el ratón por un icono: tooltip con nombre, descripción y duración.
- Las falsas de la hipocondría también salen como moodles y su tooltip parece real.

## Modo programado (temporizador)
- `/pandemonium mode scheduled`: nada se pilla de forma natural; cada 10 min (ajustable) el servidor reparte una condición aleatoria. `/pandemonium mode natural` vuelve a lo normal.
- `/pandemonium interval <minutos>`: cambia el intervalo (1-1440) y reinicia el temporizador.
- `/pandemonium timer`: cuánto falta. `/pandemonium next`: reparte ya.
- `/pandemonium target all|one`: a todos (una distinta para cada uno) o a un jugador al azar.
- `/pandemonium contagion on|off`: si las condiciones repartidas se pueden contagiar entre jugadores.
- Temporizador sincronizado arriba en el centro (con barra), aviso en chat a 1 min / 30 s / 10 s (también para quien no tenga el mod) y banner de "¡NUEVA CONDICIÓN!". La cuenta solo avanza con jugadores conectados. Los ajustes se guardan en el mundo.

## TOC contextual
Las compulsiones dependen de lo que tengas cerca, con tiempo límite; si te pasas o no llegas pierdes vida (3-4) y te da ansiedad. Si lo cumples (y te quedas quieto 2,5 s tras llegar al número), alivio y velocidad:
- salta exactamente N veces · agáchate exactamente N veces · mira al suelo 4 s
- rompe exactamente 3-4 bloques de hierba/flores/hojas/troncos/tierra/arena/piedra (si hay suficientes cerca)
- toca exactamente 3 veces un cofre/puerta/mesa de crafteo/cama/palanca cercanos
- mira fijamente 5 s a una criatura o jugador cercano

## Más efectos visuales
- Latido doble con viñeta en ataques de pánico/ansiedad; grietas de hielo que crecen en las esquinas (congelación); ondulación de calor; esporas verdes flotando (zombi 2+) y venas que laten en los bordes (zombi 3+, rabia 2+).
- Glitch de barras (tics, convulsiones, conmoción); aberración cromática en los bordes (conmoción, migraña); gotas de sudor (gripe, calor); chorretes verdes al vomitar.
- Ondas del tinnitus; eco/ping de la ceguera; respiración del asma que cierra y abre el túnel; "z" flotantes (insomnio, narcolepsia); cristales rotos al llegar una dosis.

## Modelo del jugador (lo ven todos)
Poses nuevas: encorvado (gripe, calor, congelación, asma, zombi 3+, rabia 3), cojera (esguince, zombi 4), tantear con los brazos (ceguera), rascarse (varicela), taparse la cara temblando (pánico, aracnofobia, claustrofobia), desplomarse (narcolepsia, desmayo de anemia), sacudidas bruscas (tics, convulsiones, conmoción), además de tos, vómito, temblor, brazos de zombi y torso inclinado. Las poses cortas se actualizan cada 4 ticks.
- Las poses ahora usan un muelle amortiguado: entran y salen con rebote (se pasan un poco y vuelven), no a golpe seco.

## Tanda 5
- **Arreglos de compilación** de la última prueba (sonido de lobo, swing, pantalla actual, registro de paquetes `clientboundPlay`).
- **Esquizofrenia**: pasos/creeper/susurros falsos, sombras en el rabillo del ojo y enfermedades falsas en el panel.
- **Cleptomanía**: lo que tires vuelve al inventario y los objetos cercanos te atraen (si el mixin del servidor no engancha, solo funciona la atracción).
- **Daltonismo**: ahora cambia el aspecto de los bloques con una semilla propia de cada jugador (roca→obsidiana, troncos→piedra...). Solo lo ve quien lo tiene y tiene el mod.
- **Virus contagiosos**: resfriado, gastroenteritis (vomitar a alguien contagia), sarampión, conjuntivitis, peste negra (3 fases), además de gripe, varicela, zombi y rabia. Se pasan por cercanía/golpes/tos y los frena la mascarilla.
- **Comida cruda**: carne cruda puede dar salmonela o parásitos (tenia: pierdes comida, 2 fases).
- **Más discapacidades**: brazo inutilizado, pierna lesionada, ceguera nocturna, mudez, tartamudez (chat) y vértigo.

## Rediseño (moodles, efectos y poses)
- **Moodles** a la izquierda, compactos, con casilla estilo inventario de Minecraft e iconos de 8x8 con contorno, color y brillo; tooltip estilo vanilla (fondo morado) a la derecha del icono.
- **Efectos visuales**: se quitaron los adornos que no encajaban (grietas, ondas, glitch, gotas, zzz...). Quedan solo los de borde de pantalla (viñetas) y los granitos (varicela, sarampión, peste).
- **Torso doblado de verdad**: ahora gira alrededor de la cadera; cabeza y brazos se recolocan y las piernas no se mueven. Solo se aplica a jugadores (ya no a zombis cercanos).
- **Cámara**: inclinación exacta de 15° (0,2618 rad), suavizada por tiempo, enganchada a `setup`/`update`. Si se inclina hacia el lado contrario que el torso, cambia `ROLL_SIGN` a `-1f` en `ClientState.java`.
