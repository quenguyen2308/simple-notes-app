package com.yourname.simplenotes.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class NoteDao_Impl implements NoteDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfSoftDelete;

  private final SharedSQLiteStatement __preparedStmtOfMarkClean;

  private final EntityUpsertionAdapter<NoteEntity> __upsertionAdapterOfNoteEntity;

  public NoteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfSoftDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE notes SET isDeleted = 1, isDirty = 1, updatedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkClean = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE notes SET isDirty = 0 WHERE id = ?";
        return _query;
      }
    };
    this.__upsertionAdapterOfNoteEntity = new EntityUpsertionAdapter<NoteEntity>(new EntityInsertionAdapter<NoteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `notes` (`id`,`title`,`content_blocks_json`,`folder_id`,`background_color`,`is_pinned`,`labels_json`,`metadata_json`,`createdAt`,`updatedAt`,`isDirty`,`isDeleted`,`isLocked`,`pinHash`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getContentBlocksJson());
        if (entity.getFolderId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getFolderId());
        }
        statement.bindLong(5, entity.getBackgroundColor());
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindString(7, entity.getLabelsJson());
        statement.bindString(8, entity.getMetadataJson());
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getUpdatedAt());
        final int _tmp_1 = entity.isDirty() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        final int _tmp_2 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(12, _tmp_2);
        final int _tmp_3 = entity.isLocked() ? 1 : 0;
        statement.bindLong(13, _tmp_3);
        if (entity.getPinHash() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getPinHash());
        }
      }
    }, new EntityDeletionOrUpdateAdapter<NoteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `notes` SET `id` = ?,`title` = ?,`content_blocks_json` = ?,`folder_id` = ?,`background_color` = ?,`is_pinned` = ?,`labels_json` = ?,`metadata_json` = ?,`createdAt` = ?,`updatedAt` = ?,`isDirty` = ?,`isDeleted` = ?,`isLocked` = ?,`pinHash` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getContentBlocksJson());
        if (entity.getFolderId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getFolderId());
        }
        statement.bindLong(5, entity.getBackgroundColor());
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindString(7, entity.getLabelsJson());
        statement.bindString(8, entity.getMetadataJson());
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getUpdatedAt());
        final int _tmp_1 = entity.isDirty() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        final int _tmp_2 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(12, _tmp_2);
        final int _tmp_3 = entity.isLocked() ? 1 : 0;
        statement.bindLong(13, _tmp_3);
        if (entity.getPinHash() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getPinHash());
        }
        statement.bindString(15, entity.getId());
      }
    });
  }

  @Override
  public Object softDelete(final String id, final long now,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSoftDelete.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, now);
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfSoftDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markClean(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkClean.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfMarkClean.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final NoteEntity note, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfNoteEntity.upsert(note);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<NoteEntity> notes,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfNoteEntity.upsert(notes);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<NoteEntity>> observeAll() {
    final String _sql = "SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<List<NoteEntity>>() {
      @Override
      @NonNull
      public List<NoteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContentBlocksJson = CursorUtil.getColumnIndexOrThrow(_cursor, "content_blocks_json");
          final int _cursorIndexOfFolderId = CursorUtil.getColumnIndexOrThrow(_cursor, "folder_id");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "background_color");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
          final int _cursorIndexOfLabelsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "labels_json");
          final int _cursorIndexOfMetadataJson = CursorUtil.getColumnIndexOrThrow(_cursor, "metadata_json");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfIsLocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLocked");
          final int _cursorIndexOfPinHash = CursorUtil.getColumnIndexOrThrow(_cursor, "pinHash");
          final List<NoteEntity> _result = new ArrayList<NoteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContentBlocksJson;
            _tmpContentBlocksJson = _cursor.getString(_cursorIndexOfContentBlocksJson);
            final String _tmpFolderId;
            if (_cursor.isNull(_cursorIndexOfFolderId)) {
              _tmpFolderId = null;
            } else {
              _tmpFolderId = _cursor.getString(_cursorIndexOfFolderId);
            }
            final int _tmpBackgroundColor;
            _tmpBackgroundColor = _cursor.getInt(_cursorIndexOfBackgroundColor);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final String _tmpLabelsJson;
            _tmpLabelsJson = _cursor.getString(_cursorIndexOfLabelsJson);
            final String _tmpMetadataJson;
            _tmpMetadataJson = _cursor.getString(_cursorIndexOfMetadataJson);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp_1 != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_2 != 0;
            final boolean _tmpIsLocked;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsLocked);
            _tmpIsLocked = _tmp_3 != 0;
            final String _tmpPinHash;
            if (_cursor.isNull(_cursorIndexOfPinHash)) {
              _tmpPinHash = null;
            } else {
              _tmpPinHash = _cursor.getString(_cursorIndexOfPinHash);
            }
            _item = new NoteEntity(_tmpId,_tmpTitle,_tmpContentBlocksJson,_tmpFolderId,_tmpBackgroundColor,_tmpIsPinned,_tmpLabelsJson,_tmpMetadataJson,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsDirty,_tmpIsDeleted,_tmpIsLocked,_tmpPinHash);
            _result.add(_item);
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

  @Override
  public Object getById(final String id, final Continuation<? super NoteEntity> $completion) {
    final String _sql = "SELECT * FROM notes WHERE id = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<NoteEntity>() {
      @Override
      @Nullable
      public NoteEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContentBlocksJson = CursorUtil.getColumnIndexOrThrow(_cursor, "content_blocks_json");
          final int _cursorIndexOfFolderId = CursorUtil.getColumnIndexOrThrow(_cursor, "folder_id");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "background_color");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
          final int _cursorIndexOfLabelsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "labels_json");
          final int _cursorIndexOfMetadataJson = CursorUtil.getColumnIndexOrThrow(_cursor, "metadata_json");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfIsLocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLocked");
          final int _cursorIndexOfPinHash = CursorUtil.getColumnIndexOrThrow(_cursor, "pinHash");
          final NoteEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContentBlocksJson;
            _tmpContentBlocksJson = _cursor.getString(_cursorIndexOfContentBlocksJson);
            final String _tmpFolderId;
            if (_cursor.isNull(_cursorIndexOfFolderId)) {
              _tmpFolderId = null;
            } else {
              _tmpFolderId = _cursor.getString(_cursorIndexOfFolderId);
            }
            final int _tmpBackgroundColor;
            _tmpBackgroundColor = _cursor.getInt(_cursorIndexOfBackgroundColor);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final String _tmpLabelsJson;
            _tmpLabelsJson = _cursor.getString(_cursorIndexOfLabelsJson);
            final String _tmpMetadataJson;
            _tmpMetadataJson = _cursor.getString(_cursorIndexOfMetadataJson);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp_1 != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_2 != 0;
            final boolean _tmpIsLocked;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsLocked);
            _tmpIsLocked = _tmp_3 != 0;
            final String _tmpPinHash;
            if (_cursor.isNull(_cursorIndexOfPinHash)) {
              _tmpPinHash = null;
            } else {
              _tmpPinHash = _cursor.getString(_cursorIndexOfPinHash);
            }
            _result = new NoteEntity(_tmpId,_tmpTitle,_tmpContentBlocksJson,_tmpFolderId,_tmpBackgroundColor,_tmpIsPinned,_tmpLabelsJson,_tmpMetadataJson,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsDirty,_tmpIsDeleted,_tmpIsLocked,_tmpPinHash);
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
  public Object getDirtyNotes(final Continuation<? super List<NoteEntity>> $completion) {
    final String _sql = "SELECT * FROM notes WHERE isDirty = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<NoteEntity>>() {
      @Override
      @NonNull
      public List<NoteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContentBlocksJson = CursorUtil.getColumnIndexOrThrow(_cursor, "content_blocks_json");
          final int _cursorIndexOfFolderId = CursorUtil.getColumnIndexOrThrow(_cursor, "folder_id");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "background_color");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
          final int _cursorIndexOfLabelsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "labels_json");
          final int _cursorIndexOfMetadataJson = CursorUtil.getColumnIndexOrThrow(_cursor, "metadata_json");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfIsLocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLocked");
          final int _cursorIndexOfPinHash = CursorUtil.getColumnIndexOrThrow(_cursor, "pinHash");
          final List<NoteEntity> _result = new ArrayList<NoteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContentBlocksJson;
            _tmpContentBlocksJson = _cursor.getString(_cursorIndexOfContentBlocksJson);
            final String _tmpFolderId;
            if (_cursor.isNull(_cursorIndexOfFolderId)) {
              _tmpFolderId = null;
            } else {
              _tmpFolderId = _cursor.getString(_cursorIndexOfFolderId);
            }
            final int _tmpBackgroundColor;
            _tmpBackgroundColor = _cursor.getInt(_cursorIndexOfBackgroundColor);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final String _tmpLabelsJson;
            _tmpLabelsJson = _cursor.getString(_cursorIndexOfLabelsJson);
            final String _tmpMetadataJson;
            _tmpMetadataJson = _cursor.getString(_cursorIndexOfMetadataJson);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp_1 != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_2 != 0;
            final boolean _tmpIsLocked;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsLocked);
            _tmpIsLocked = _tmp_3 != 0;
            final String _tmpPinHash;
            if (_cursor.isNull(_cursorIndexOfPinHash)) {
              _tmpPinHash = null;
            } else {
              _tmpPinHash = _cursor.getString(_cursorIndexOfPinHash);
            }
            _item = new NoteEntity(_tmpId,_tmpTitle,_tmpContentBlocksJson,_tmpFolderId,_tmpBackgroundColor,_tmpIsPinned,_tmpLabelsJson,_tmpMetadataJson,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsDirty,_tmpIsDeleted,_tmpIsLocked,_tmpPinHash);
            _result.add(_item);
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
  public Object getByIds(final List<String> ids,
      final Continuation<? super List<NoteEntity>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM notes WHERE id IN (");
    final int _inputSize = ids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(") AND isDeleted = 0");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : ids) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<NoteEntity>>() {
      @Override
      @NonNull
      public List<NoteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContentBlocksJson = CursorUtil.getColumnIndexOrThrow(_cursor, "content_blocks_json");
          final int _cursorIndexOfFolderId = CursorUtil.getColumnIndexOrThrow(_cursor, "folder_id");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "background_color");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
          final int _cursorIndexOfLabelsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "labels_json");
          final int _cursorIndexOfMetadataJson = CursorUtil.getColumnIndexOrThrow(_cursor, "metadata_json");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfIsLocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLocked");
          final int _cursorIndexOfPinHash = CursorUtil.getColumnIndexOrThrow(_cursor, "pinHash");
          final List<NoteEntity> _result = new ArrayList<NoteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteEntity _item_1;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContentBlocksJson;
            _tmpContentBlocksJson = _cursor.getString(_cursorIndexOfContentBlocksJson);
            final String _tmpFolderId;
            if (_cursor.isNull(_cursorIndexOfFolderId)) {
              _tmpFolderId = null;
            } else {
              _tmpFolderId = _cursor.getString(_cursorIndexOfFolderId);
            }
            final int _tmpBackgroundColor;
            _tmpBackgroundColor = _cursor.getInt(_cursorIndexOfBackgroundColor);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final String _tmpLabelsJson;
            _tmpLabelsJson = _cursor.getString(_cursorIndexOfLabelsJson);
            final String _tmpMetadataJson;
            _tmpMetadataJson = _cursor.getString(_cursorIndexOfMetadataJson);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp_1 != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_2 != 0;
            final boolean _tmpIsLocked;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsLocked);
            _tmpIsLocked = _tmp_3 != 0;
            final String _tmpPinHash;
            if (_cursor.isNull(_cursorIndexOfPinHash)) {
              _tmpPinHash = null;
            } else {
              _tmpPinHash = _cursor.getString(_cursorIndexOfPinHash);
            }
            _item_1 = new NoteEntity(_tmpId,_tmpTitle,_tmpContentBlocksJson,_tmpFolderId,_tmpBackgroundColor,_tmpIsPinned,_tmpLabelsJson,_tmpMetadataJson,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsDirty,_tmpIsDeleted,_tmpIsLocked,_tmpPinHash);
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
