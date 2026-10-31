CREATE OR REPLACE FUNCTION fn_auditar_paciente()
RETURNS TRIGGER AS $$
DECLARE
    v_usuario BIGINT;
BEGIN
    BEGIN
        v_usuario := NULLIF(current_setting('app.user_id', true), '')::BIGINT;
    EXCEPTION WHEN OTHERS THEN
        v_usuario := NULL;
    END;

    INSERT INTO auditoria(id_usuario, entidad, id_entidad, accion, valor_anterior, valor_nuevo)
    VALUES (
        v_usuario,
        'paciente',
        COALESCE(NEW.id_paciente, OLD.id_paciente),
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN to_jsonb(OLD) ELSE NULL END,
        CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN to_jsonb(NEW) ELSE NULL END
    );

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditoria_paciente ON paciente;

CREATE TRIGGER trg_auditoria_paciente
AFTER INSERT OR UPDATE OR DELETE ON paciente
FOR EACH ROW EXECUTE FUNCTION fn_auditar_paciente();
