import {Connectable} from "../../../application/repository";
import {Connection, ConnectionOptions, createConnection} from "mysql2/promise";

const ER_NO_REFERENCED_ROW_2 = "ER_NO_REFERENCED_ROW_2"

export abstract class SqlConnection implements Connectable {
    protected connection?: Connection;

    async connect(config: ConnectionOptions) {
        this.connection = await createConnection(config);
    }
}

export abstract class SqlErrors extends SqlConnection{
    protected throwErrorFor(error: any): void {
        switch (error.code) {
            case ER_NO_REFERENCED_ROW_2:
                throw new NoReferencedRowError(
                    error.message,
                    error.code,
                    error.errno,
                    error.sqlState,
                    error.sqlMessage
                )
            default: throw error;
        }
    }
}

export class NoReferencedRowError extends Error {
    code?: string;
    errno?: number;
    sqlState?: string;
    sqlMessage?: string;

    constructor(message: string, code?: string, errno?: number, sqlState?: string, sqlMessage?: string) {
        super(message);
        this.name = 'NoReferencedRowError';
        this.code = code;
        this.errno = errno;
        this.sqlState = sqlState;
        this.sqlMessage = sqlMessage;

        if (Error.captureStackTrace) {
            Error.captureStackTrace(this, NoReferencedRowError);
        }
    }
}
