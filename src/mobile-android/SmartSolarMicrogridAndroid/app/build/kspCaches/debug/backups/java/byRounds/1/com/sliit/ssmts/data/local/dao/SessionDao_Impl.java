package com.sliit.ssmts.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.sliit.ssmts.data.local.entity.SessionEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SessionDao_Impl implements SessionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SessionEntity> __insertionAdapterOfSessionEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateProfileCache;

  private final SharedSQLiteStatement __preparedStmtOfUpdateStatusCache;

  private final SharedSQLiteStatement __preparedStmtOfClearSession;

  public SessionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSessionEntity = new EntityInsertionAdapter<SessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_session` (`id`,`token`,`userId`,`nic`,`username`,`fullName`,`phone`,`address`,`latitude`,`longitude`,`role`,`status`,`expiresAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SessionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getToken());
        statement.bindString(3, entity.getUserId());
        statement.bindString(4, entity.getNic());
        statement.bindString(5, entity.getUsername());
        statement.bindString(6, entity.getFullName());
        statement.bindString(7, entity.getPhone());
        statement.bindString(8, entity.getAddress());
        if (entity.getLatitude() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getLatitude());
        }
        if (entity.getLongitude() == null) {
          statement.bindNull(10);
        } else {
          statement.bindDouble(10, entity.getLongitude());
        }
        statement.bindString(11, entity.getRole());
        statement.bindString(12, entity.getStatus());
        if (entity.getExpiresAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getExpiresAt());
        }
        statement.bindLong(14, entity.getUpdatedAt());
      }
    };
    this.__preparedStmtOfUpdateProfileCache = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_session SET fullName = ?, phone = ?, address = ?, updatedAt = ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateStatusCache = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_session SET status = ?, updatedAt = ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfClearSession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM user_session";
        return _query;
      }
    };
  }

  @Override
  public Object saveSession(final SessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSessionEntity.insert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateProfileCache(final String fullName, final String phone, final String address,
      final long updatedAt, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateProfileCache.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, fullName);
        _argIndex = 2;
        _stmt.bindString(_argIndex, phone);
        _argIndex = 3;
        _stmt.bindString(_argIndex, address);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, updatedAt);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateProfileCache.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateStatusCache(final String status, final long updatedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateStatusCache.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, updatedAt);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateStatusCache.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearSession(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearSession.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearSession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getActiveSession(final Continuation<? super SessionEntity> $completion) {
    final String _sql = "SELECT * FROM user_session WHERE id = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SessionEntity>() {
      @Override
      @Nullable
      public SessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfToken = CursorUtil.getColumnIndexOrThrow(_cursor, "token");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfNic = CursorUtil.getColumnIndexOrThrow(_cursor, "nic");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfFullName = CursorUtil.getColumnIndexOrThrow(_cursor, "fullName");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfExpiresAt = CursorUtil.getColumnIndexOrThrow(_cursor, "expiresAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final SessionEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpToken;
            _tmpToken = _cursor.getString(_cursorIndexOfToken);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpNic;
            _tmpNic = _cursor.getString(_cursorIndexOfNic);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpFullName;
            _tmpFullName = _cursor.getString(_cursorIndexOfFullName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpExpiresAt;
            if (_cursor.isNull(_cursorIndexOfExpiresAt)) {
              _tmpExpiresAt = null;
            } else {
              _tmpExpiresAt = _cursor.getString(_cursorIndexOfExpiresAt);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new SessionEntity(_tmpId,_tmpToken,_tmpUserId,_tmpNic,_tmpUsername,_tmpFullName,_tmpPhone,_tmpAddress,_tmpLatitude,_tmpLongitude,_tmpRole,_tmpStatus,_tmpExpiresAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<SessionEntity> observeActiveSession() {
    final String _sql = "SELECT * FROM user_session WHERE id = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_session"}, new Callable<SessionEntity>() {
      @Override
      @Nullable
      public SessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfToken = CursorUtil.getColumnIndexOrThrow(_cursor, "token");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfNic = CursorUtil.getColumnIndexOrThrow(_cursor, "nic");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfFullName = CursorUtil.getColumnIndexOrThrow(_cursor, "fullName");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfExpiresAt = CursorUtil.getColumnIndexOrThrow(_cursor, "expiresAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final SessionEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpToken;
            _tmpToken = _cursor.getString(_cursorIndexOfToken);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpNic;
            _tmpNic = _cursor.getString(_cursorIndexOfNic);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpFullName;
            _tmpFullName = _cursor.getString(_cursorIndexOfFullName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpExpiresAt;
            if (_cursor.isNull(_cursorIndexOfExpiresAt)) {
              _tmpExpiresAt = null;
            } else {
              _tmpExpiresAt = _cursor.getString(_cursorIndexOfExpiresAt);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new SessionEntity(_tmpId,_tmpToken,_tmpUserId,_tmpNic,_tmpUsername,_tmpFullName,_tmpPhone,_tmpAddress,_tmpLatitude,_tmpLongitude,_tmpRole,_tmpStatus,_tmpExpiresAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
