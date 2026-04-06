CREATE TABLE financial_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(19,4) NOT NULL,
    type VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    date DATE NOT NULL,
    notes TEXT,
    deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_records_user    ON financial_records(created_by);
CREATE INDEX idx_records_date    ON financial_records(date);
CREATE INDEX idx_records_type    ON financial_records(type);
CREATE INDEX idx_records_deleted ON financial_records(deleted);