SELECT set_config('mentenova.usuario_app', :'usuario_app', false);

GRANT USAGE, CREATE ON SCHEMA public TO :"usuario_app";

DO $$
DECLARE
    v_usuario_app text := current_setting('mentenova.usuario_app');
    r record;
BEGIN
    FOR r IN
        SELECT c.relname
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public'
          AND c.relkind IN ('r', 'p', 'v')
          AND pg_get_userbyid(c.relowner) <> v_usuario_app
    LOOP
        EXECUTE format('ALTER TABLE public.%I OWNER TO %I', r.relname, v_usuario_app);
        RAISE NOTICE 'Tabla % ahora pertenece a %', r.relname, v_usuario_app;
    END LOOP;

    FOR r IN
        SELECT p.oid::regprocedure AS firma
        FROM pg_proc p
        JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname = 'public'
          AND pg_get_userbyid(p.proowner) <> v_usuario_app
    LOOP
        EXECUTE format('ALTER FUNCTION %s OWNER TO %I', r.firma, v_usuario_app);
        RAISE NOTICE 'Funcion % ahora pertenece a %', r.firma, v_usuario_app;
    END LOOP;
END $$;
