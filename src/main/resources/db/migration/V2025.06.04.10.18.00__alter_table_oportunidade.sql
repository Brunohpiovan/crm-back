ALTER TABLE oportunidade
DROP CHECK check_oportunidade_origem,
ADD CONSTRAINT check_oportunidade_origem CHECK (origem IN ('EMAIL','FACEBOOK','INSTAGRAM','OUTRO','WHATSAPP','LINKEDIN','SITE'));