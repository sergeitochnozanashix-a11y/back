ALTER TABLE public.modules
    ADD COLUMN IF NOT EXISTS course_id BIGINT;

DO
$$
    BEGIN
        IF to_regclass('public.lessons') IS NOT NULL THEN
            DELETE
            FROM public.lessons l
                USING public.modules m
            WHERE l.module_id = m.id
              AND m.course_id IS NULL;
        END IF;
    END
$$;

DELETE
FROM public.modules m
WHERE m.course_id IS NULL;

DO
$$
    BEGIN
        IF to_regclass('public.lessons') IS NOT NULL THEN
            DELETE
            FROM public.lessons l
                USING public.modules m
                    LEFT JOIN public.courses c ON c.id = m.course_id
            WHERE l.module_id = m.id
              AND m.course_id IS NOT NULL
              AND c.id IS NULL;
        END IF;
    END
$$;

DELETE
FROM public.modules m
WHERE m.course_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM public.courses c WHERE c.id = m.course_id);

CREATE INDEX IF NOT EXISTS idx_modules_course_id ON public.modules (course_id);

ALTER TABLE public.modules
    ALTER COLUMN course_id SET NOT NULL;
