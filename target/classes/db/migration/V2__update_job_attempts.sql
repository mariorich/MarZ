-- Store both JES identifiers so MarZ can retrieve job status.
ALTER TABLE job_attempts
    ADD COLUMN jes_job_name VARCHAR(8);

-- JES return codes can contain values other than numeric condition codes,
-- such as JCL ERROR or ABEND S0C7.
ALTER TABLE job_attempts
    ALTER COLUMN return_code TYPE VARCHAR(20)
    USING return_code::VARCHAR(20);
