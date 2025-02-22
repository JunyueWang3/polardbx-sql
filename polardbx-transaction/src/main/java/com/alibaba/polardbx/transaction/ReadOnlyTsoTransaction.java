/*
 * Copyright [2013-2021], Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.polardbx.transaction;

import com.alibaba.polardbx.common.jdbc.IConnection;
import com.alibaba.polardbx.common.jdbc.IDataSource;
import com.alibaba.polardbx.common.jdbc.ITransactionPolicy;
import com.alibaba.polardbx.common.jdbc.MasterSlave;
import com.alibaba.polardbx.common.properties.ConnectionParams;
import com.alibaba.polardbx.common.utils.logger.Logger;
import com.alibaba.polardbx.common.utils.logger.LoggerFactory;
import com.alibaba.polardbx.executor.utils.ExecUtils;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.utils.ITransaction;
import com.alibaba.polardbx.rpc.pool.XConnection;
import com.alibaba.polardbx.transaction.async.AsyncTaskQueue;
import com.alibaba.polardbx.transaction.jdbc.DeferredConnection;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhuangtianyi
 */
public class ReadOnlyTsoTransaction extends AutoCommitTransaction implements ITsoTransaction {
    private final static Logger logger = LoggerFactory.getLogger(ReadOnlyTsoTransaction.class);

    private long snapshotTimestamp = -1;
    private final ConcurrentHashMap<String, Long> dnLsnMap = new ConcurrentHashMap<>();

    public ReadOnlyTsoTransaction(ExecutionContext executionContext,
                                  TransactionManager manager) {
        super(executionContext, manager);

        final String schemaName = executionContext.getSchemaName();
        if (!StringUtils.isEmpty(schemaName)) {
            TransactionManager.getInstance(schemaName).enableXaRecoverScan();
            TransactionManager.getInstance(schemaName).enableKillTimeoutTransaction();
        }
    }

    @Override
    public long getSnapshotSeq() {
        if (snapshotTimestamp < 0) {
            snapshotTimestamp = nextTimestamp();
        }
        return snapshotTimestamp;
    }

    @Override
    public boolean snapshotSeqIsEmpty() {
        return snapshotTimestamp <= 0;
    }

    @Override
    public void updateSnapshotTimestamp() {
        if (!this.autoCommit && isolationLevel == Connection.TRANSACTION_READ_COMMITTED) {
            snapshotTimestamp = nextTimestamp();
        }
    }

    @Override
    public IConnection getConnection(String schemaName, String group, IDataSource ds, RW rw, ExecutionContext ec)
        throws SQLException {
        MasterSlave masterSlave = ExecUtils.getMasterSlave(
            false, rw.equals(ITransaction.RW.WRITE), executionContext);
        IConnection conn = super.getRealConnection(schemaName, group, ds, masterSlave);
        conn = new DeferredConnection(conn, ec.getParamManager().getBoolean(
            ConnectionParams.USING_RDS_RESULT_SKIP));

        /**
         * Here must get TSO for slave connection before fetch the LSN!
         */
        lock.lock();
        try {
            getSnapshotSeq();
        } finally {
            lock.unlock();
        }
        conn = sendLsn(conn, schemaName, group, masterSlave, this::getSnapshotSeq);
        sendSnapshotSeq(conn);
        return conn;
    }

    @Override
    public void tryClose() throws SQLException {
        if (isClosed()) {
            return;
        }

        // TConnection 每次执行完语句会调用 tryClose
        if (autoCommit) {
            rollback();
        }
    }

    @Override
    public void tryClose(IConnection conn, String groupName) throws SQLException {
        try {
            if (conn.isWrapperFor(XConnection.class)) {
                super.tryClose(conn, groupName);
                return;
            }
        } catch (SQLException ignored) {
            // Is jdbc connection
        }

        lock.lock();
        try {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ROLLBACK");
            } catch (Throwable e) {
                logger.error("Cleanup readonly transaction branch failed on " + groupName, e);
                discard(groupName, conn, e);
                return;
            }

            this.getConnectionHolder().tryClose(conn, groupName);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        AsyncTaskQueue asyncQueue = getManager().getTransactionExecutor().getAsyncQueue();
        ch.forEachConnection(asyncQueue, (conn) -> {

            try {
                if (conn.isClosed() || conn.isWrapperFor(XConnection.class)) {
                    return;
                }
            } catch (SQLException ignored) {
                // Is jdbc connection
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ROLLBACK");
            } catch (Throwable e) {
                logger.error("Cleanup readonly transaction branch failed!", e);
                discard("", conn, e);
            }
        });

        super.close();
    }

    @Override
    public ITransactionPolicy.TransactionClass getTransactionClass() {
        return ITransactionPolicy.TransactionClass.TSO_READONLY;
    }
}
