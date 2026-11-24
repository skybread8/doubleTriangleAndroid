# Auditoría de traducciones: reservas

La clave compartida `Time` se usa en iOS para la hora de una reserva, pero la exportación española contiene `tiempo`, que en ese contexto resulta ambigua. Android usa `booking_time_of_day` con una traducción local para los diez idiomas; conviene añadir una clave específica para hora de reserva al Sheet.

Las celdas ausentes del resto de claves exportadas se detallan en `sheet-missing-cells.tsv`.
