package com.demo.haima.common.definition;

import com.demo.haima.common.exception.HaimaException;
import com.demo.haima.common.serdes.jute.statistics.Stat;

/**
 * Encodes the result of a single part of a multiple operation commit.
 * 
 * @author Vince Yuan
 * @date 2021/11/11
 */
public abstract class OpResult {

    private int type;

    private OpResult(int type) {
        this.type = type;
    }

    /**
     * Encodes the return type as from OpCode.  Can be used
     * to dispatch to the correct cast needed for getting the desired
     * additional result data.
     * @return an integer identifying what kind of operation this result came from.
     */
    public int getType() {
        return type;
    }

    /**
     * A result from a create operation.  This kind of result allows the
     * path to be retrieved since the create might have been a sequential
     * create.
     */
    public static class CreateResult extends OpResult {
        private String path;

        public CreateResult(String path) {
            super(OpCode.create);
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CreateResult)) {
                return false;
            }

            CreateResult other = (CreateResult) o;
            return getType() == other.getType() && path.equals(other.getPath());
        }

        @Override
        public int hashCode() {
            return getType() * 35 + path.hashCode();
        }
    }

    /**
     * A result from a delete operation.  No special values are available.
     */
    public static class DeleteResult extends OpResult {
        public DeleteResult() {
            super(OpCode.delete);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DeleteResult)) {
                return false;
            }

            DeleteResult opResult = (DeleteResult) o;
            return getType() == opResult.getType();
        }

        @Override
        public int hashCode() {
            return getType();
        }
    }

    /**
     * A result from a setData operation.  This kind of result provides access
     * to the Stat structure from the update.
     */
    public static class SetDataResult extends OpResult {
        private Stat stat;

        public SetDataResult(Stat stat) {
            super(OpCode.setData);
            this.stat = stat;
        }

        public Stat getStat() {
            return stat;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SetDataResult)) {
                return false;
            }

            SetDataResult other = (SetDataResult) o;
            return getType() == other.getType() && stat.getMzxid() == other.stat.getMzxid();
        }

        @Override
        public int hashCode() {
            return (int) (getType() * 35 + stat.getMzxid());
        }
    }

    /**
     * A result from a version check operation.  No special values are available.
     */
    public static class CheckResult extends OpResult {
        public CheckResult() {
            super(OpCode.check);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CheckResult)) {
                return false;
            }

            CheckResult other = (CheckResult) o;
            return getType() == other.getType();
        }

        @Override
        public int hashCode() {
            return getType();
        }
    }

    /**
     * An error result from any kind of operation. The point of error results
     * is that they contain an error code which helps understand what happened.
     * @see HaimaException.Code
     */
    public static class ErrorResult extends OpResult {
        private int err;

        public ErrorResult(int err) {
            super(OpCode.error);
            this.err = err;
        }

        public int getErr() {
            return err;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ErrorResult)) {
                return false;
            }

            ErrorResult other = (ErrorResult) o;
            return getType() == other.getType() && err == other.getErr();
        }

        @Override
        public int hashCode() {
            return getType() * 35 + err;
        }
    }
}
