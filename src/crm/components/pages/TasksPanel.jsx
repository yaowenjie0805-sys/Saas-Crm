import { translateTaskLevel, translateTimeLabel } from '../../shared'

function TasksPanel({ activePage, t, tasks, canWrite, toggleTaskDone }) {
  if (activePage !== 'tasks') return null

  return (
    <section className="panel">
      <div className="panel-head"><h2>{t('tasks')}</h2></div>
      {tasks.slice(0, 10).map((task) => (
        <div key={task.id} className={`task-item ${task.done ? 'done' : ''}`}>
          <div>
            <strong>{task.title}</strong>
            <p>{translateTimeLabel(t, task.time)} | {translateTaskLevel(t, task.level)} | {t('owner')}: {task.owner || '-'}</p>
          </div>
          <button className="mini-btn" disabled={!canWrite} onClick={() => toggleTaskDone(task)}>{task.done ? t('undo') : t('done')}</button>
        </div>
      ))}
    </section>
  )
}

export default TasksPanel
