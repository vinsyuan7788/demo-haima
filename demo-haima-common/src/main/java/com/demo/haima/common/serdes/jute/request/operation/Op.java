package com.demo.haima.common.serdes.jute.request.operation;

import com.demo.haima.common.definition.CreateMode;
import com.demo.haima.common.definition.OpCode;
import com.demo.haima.common.exception.HaimaException;
import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.request.CheckVersionRequest;
import com.demo.haima.common.serdes.jute.request.CreateRequest;
import com.demo.haima.common.serdes.jute.request.DeleteRequest;
import com.demo.haima.common.serdes.jute.request.SetDataRequest;
import com.demo.haima.common.utility.PathUtils;

import java.util.Arrays;

/**
 * Represents a single operation in a multi-operation transaction.  Each operation can be a create, update
 * or delete or can just be a version check.
 *
 * Sub-classes of Op each represent each detailed type but should not normally be referenced except via
 * the provided factory methods.
 *
 * @author Vince Yuan
 * @date 2021/11/11
 */
public abstract class Op {
    private int type;
    private String path;

    // prevent untyped construction
    private Op(int type, String path) {
        this.type = type;
        this.path = path;
    }

    /**
     * Constructs a create operation.  Arguments are as for the Haima method of the same name.
     *
     * @param path
     *                the path for the node
     * @param data
     *                the initial data for the node
     * @param flags
     *                specifying whether the node to be created is
     *                sequential but using the integer encoding.
     */
    public static Op create(String path, byte[] data, int flags) {
        return new Create(path, data, flags);
    }

    /**
     * Constructs a create operation.  Arguments are as for the Haima method of the same name.
     *
     * @param path
     *                the path for the node
     * @param data
     *                the initial data for the node
     * @param createMode
     *                specifying whether the node to be created is sequential
     */
    public static Op create(String path, byte[] data, CreateMode createMode) {
        return new Create(path, data, createMode);
    }

    /**
     * Constructs a delete operation.  Arguments are as for the Haima method of the same name.
     *
     * @param path
     *                the path of the node to be deleted.
     * @param version
     *                the expected node version.
     */
    public static Op delete(String path, int version) {
        return new Delete(path, version);
    }

    /**
     * Constructs an update operation.  Arguments are as for the Haima method of the same name.
     *
     * @param path
     *                the path of the node
     * @param data
     *                the data to set
     * @param version
     *                the expected matching version
     */
    public static Op setData(String path, byte[] data, int version) {
        return new SetData(path, data, version);
    }


    /**
     * Constructs an version check operation.  Arguments are as for the Haima setData method except that
     * no data is provided since no update is intended.  The purpose for this is to allow read-modify-write
     * operations that apply to multiple nodes, but where some of the nodes are involved only in the read,
     * not the write.  A similar effect could be achieved by writing the same data back, but that leads to
     * way more version updates than are necessary and more writing in general.
     *
     * @param path
     *                the path of the node
     * @param version
     *                the expected matching version
     */
    public static Op check(String path, int version) {
        return new Check(path, version);
    }

    /**
     * Gets the integer type code for an Op.  This code should be as from ZooDefs.OpCode
     * @see OpCode
     * @return  The type code.
     */
    public int getType() {
        return type;
    }

    /**
     * Gets the path for an Op.
     * @return  The path.
     */
    public String getPath() {
        return path;
    }

    /**
     * Encodes an op for wire transmission.
     * @return An appropriate Record structure.
     */
    public abstract Record toRequestRecord() ;

    /**
     * Reconstructs the transaction with the chroot prefix.
     *
     * @return transaction with chroot.
     */
    abstract Op withChroot(String addRootPrefix);

    /**
     * Performs client path validations.
     *
     * @throws IllegalArgumentException
     *             if an invalid path is specified
     * @throws HaimaException.BadArgumentsException
     *             if an invalid create mode flag is specified
     */
    void validate() throws HaimaException {
        PathUtils.validatePath(path);
    }

    //////////////////
    // these internal classes are public, but should not generally be referenced.
    //
    public static class Create extends Op {
        private byte[] data;
        private int flags;

        private Create(String path, byte[] data, int flags) {
            super(OpCode.create, path);
            this.data = data;
            this.flags = flags;
        }

        private Create(String path, byte[] data, CreateMode createMode) {
            super(OpCode.create, path);
            this.data = data;
            this.flags = createMode.toFlag();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Create)) {
                return false;
            }

            Create op = (Create) o;

            boolean aclEquals = true;
            return getType() == op.getType() && Arrays.equals(data, op.data) && flags == op.flags && aclEquals;
        }

        @Override
        public int hashCode() {
            return getType() + getPath().hashCode() + Arrays.hashCode(data);
        }

        @Override
        public Record toRequestRecord() {
            return new CreateRequest(getPath(), data, flags);
        }

        @Override
        Op withChroot(String path) {
            return new Create(path, data, flags);
        }

        @Override
        void validate() throws HaimaException {
            CreateMode createMode = CreateMode.fromFlag(flags);
            PathUtils.validatePath(getPath(), createMode.isSequential());
        }
    }

    public static class Delete extends Op {
        private int version;

        private Delete(String path, int version) {
            super(OpCode.delete, path);
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Delete)) {
                return false;
            }

            Delete op = (Delete) o;

            return getType() == op.getType() && version == op.version
                    && getPath().equals(op.getPath());
        }

        @Override
        public int hashCode() {
            return getType() + getPath().hashCode() + version;
        }

        @Override
        public Record toRequestRecord() {
            return new DeleteRequest(getPath(), version);
        }

        @Override
        Op withChroot(String path) {
            return new Delete(path, version);
        }
    }

    public static class SetData extends Op {
        private byte[] data;
        private int version;

        private SetData(String path, byte[] data, int version) {
            super(OpCode.setData, path);
            this.data = data;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SetData)) {
                return false;
            }

            SetData op = (SetData) o;

            return getType() == op.getType() && version == op.version
                    && getPath().equals(op.getPath()) && Arrays.equals(data, op.data);
        }

        @Override
        public int hashCode() {
            return getType() + getPath().hashCode() + Arrays.hashCode(data) + version;
        }

        @Override
        public Record toRequestRecord() {
            return new SetDataRequest(getPath(), data, version);
        }

        @Override
        Op withChroot(String path) {
            return new SetData(path, data, version);
        }
    }

    public static class Check extends Op {
        private int version;

        private Check(String path, int version) {
            super(OpCode.check, path);
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Check)) {
                return false;
            }

            Check op = (Check) o;

            return getType() == op.getType() && getPath().equals(op.getPath()) && version == op.version;
        }

        @Override
        public int hashCode() {
            return getType() + getPath().hashCode() + version;
        }

        @Override
        public Record toRequestRecord() {
            return new CheckVersionRequest(getPath(), version);
        }

        @Override
        Op withChroot(String path) {
            return new Check(path, version);
        }
    }
}

