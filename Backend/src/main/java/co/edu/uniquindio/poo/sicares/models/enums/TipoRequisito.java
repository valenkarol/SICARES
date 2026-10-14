package co.edu.uniquindio.poo.sicares.models.enums;
/**
 * Cómo se interpreta Requisito.valorEsperado según el tipo:
 *  EDAD               -> rango "min-max", ej. "15-28"
 *  NIVEL_EDUCATIVO    -> nombre de NivelEducativo, ej. "MEDIA"
 *  CONDICION_VICTIMA  -> "RUV" (debe estar inscrito en el Registro Único de Víctimas)
 *  SISBEN             -> grupos permitidos separados por coma, ej. "A,B,C"
 *  DOCUMENTO / OTRO   -> texto libre informativo
 */
public enum TipoRequisito {
    DOCUMENTO,
    EDAD,
    NIVEL_EDUCATIVO,
    CONDICION_VICTIMA,
    SISBEN,
    OTRO
}
