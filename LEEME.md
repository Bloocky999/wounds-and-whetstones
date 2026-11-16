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
