import type { ReactNode } from 'react';
import './legal.scss';

type LegalKind = 'privacy' | 'terms';

const COPY: Record<
  LegalKind,
  { title: string; updated: string; body: ReactNode }
> = {
  privacy: {
    title: '隐私政策',
    updated: '2026-09-12',
    body: (
      <>
        <p>
          欢迎使用糊涂健康（miyf，下称「我们」）相关服务。本政策说明我们如何收集、使用、存储与保护您的个人信息，
          特别是通过华为运动健康（Health Kit）等第三方授权同步的健康数据。
        </p>
        <h2>1. 我们收集的信息</h2>
        <ul>
          <li>账号信息：如登录账号、昵称、联系方式（若您主动提供）。</li>
          <li>
            健康相关数据：在您明确授权后，可能包括体重、身高、体脂率、心率、步数、血压、血糖等指标及测量时间。
          </li>
          <li>服务日志：为保障安全与排查故障，可能记录必要的操作与接口调用日志。</li>
        </ul>
        <h2>2. 收集与使用目的</h2>
        <ul>
          <li>向您或您授权的管理员展示健康趋势、样本与同步状态。</li>
          <li>完成与华为 Health Kit 等数据源的授权绑定、令牌刷新与数据同步。</li>
          <li>保障服务安全、改进产品体验，并履行法律法规要求。</li>
        </ul>
        <h2>3. 授权与第三方</h2>
        <p>
          当您通过华为账号授权本服务读取 Health Kit 数据时，我们仅在授权范围内获取数据，不会出售您的个人健康信息。
          第三方（如华为）按其自身隐私政策处理您在其平台上的数据。
        </p>
        <h2>4. 存储与安全</h2>
        <p>
          我们采取合理的技术与管理措施保护数据，包括访问控制、传输加密与最小权限原则。
          OAuth 访问令牌等敏感凭据按系统设计安全存储，并限制访问范围。
        </p>
        <h2>5. 您的权利</h2>
        <ul>
          <li>查询、更正您的账号与相关资料。</li>
          <li>撤回华为等第三方授权，或请求停止同步。</li>
          <li>在符合法律规定的情况下，请求删除相关数据。</li>
        </ul>
        <h2>6. 未成年人</h2>
        <p>若涉及未成年人健康数据，应在监护人同意与法律允许的前提下使用本服务。</p>
        <h2>7. 政策更新</h2>
        <p>我们可能适时更新本政策；更新后将在本页面公布。重大变更时，我们会通过合理方式提示您。</p>
        <h2>8. 联系我们</h2>
        <p>
          如对本政策有疑问，请通过站点公示的联系方式或管理后台运营方联系我们。站点：
          <a href="https://www.miyf.cn">https://www.miyf.cn</a>
        </p>
      </>
    ),
  },
  terms: {
    title: '用户协议',
    updated: '2026-09-12',
    body: (
      <>
        <p>
          本协议是您与糊涂健康（miyf，下称「我们」）之间关于使用本网站、管理后台、小程序及相关服务的法律协议。
          使用本服务即表示您已阅读并同意本协议及《隐私政策》。
        </p>
        <h2>1. 服务内容</h2>
        <p>
          我们提供家庭厨房、健康数据管理等信息化服务，包括但不限于菜品与订单管理、健康主体与采样数据展示，
          以及在您授权后对接华为运动健康（Health Kit）等数据源进行同步。
        </p>
        <h2>2. 账号与安全</h2>
        <ul>
          <li>您应妥善保管账号与凭证，对账号下的操作负责。</li>
          <li>如发现未经授权使用，请及时通知我们并采取合理措施。</li>
          <li>管理员账号仅限授权人员使用，不得转借给无关第三方。</li>
        </ul>
        <h2>3. 健康数据与授权</h2>
        <ul>
          <li>健康数据同步需您（或合法监护人/授权管理人）明确授权。</li>
          <li>您可随时在华为侧或本服务内撤回授权；撤回后我们将停止新增同步。</li>
          <li>本服务提供的健康信息展示不构成医疗诊断或诊疗建议，如有不适请寻求专业医疗机构帮助。</li>
        </ul>
        <h2>4. 使用规范</h2>
        <ul>
          <li>不得利用本服务从事违法违规活动，或侵害他人合法权益。</li>
          <li>不得对系统进行未授权访问、攻击、爬取或破坏。</li>
          <li>不得上传虚假、侵权或含有恶意代码的内容。</li>
        </ul>
        <h2>5. 知识产权</h2>
        <p>
          本服务中的软件、界面、文案与商标等权益归我们或相关权利人所有。未经许可，不得复制、传播或用于商业目的。
        </p>
        <h2>6. 免责与责任限制</h2>
        <p>
          在法律允许的范围内，因不可抗力、网络故障、第三方服务（含华为平台）中断或您自身原因导致的服务异常，
          我们将在合理范围内协助处理，但不承担由此产生的间接损失。
        </p>
        <h2>7. 协议变更与终止</h2>
        <p>
          我们可能更新本协议并在本页面公布。若您继续使用服务，视为接受更新后的协议。
          我们也可在必要时中止或终止向特定账号提供服务。
        </p>
        <h2>8. 适用法律</h2>
        <p>本协议适用中华人民共和国法律。争议协商不成的，提交有管辖权的人民法院解决。</p>
        <h2>9. 联系我们</h2>
        <p>
          站点：<a href="https://www.miyf.cn">https://www.miyf.cn</a>
        </p>
      </>
    ),
  },
};

/** 公开法律页：无需登录。 */
export default function LegalPage({ kind }: { kind: LegalKind }) {
  const doc = COPY[kind];
  const other = kind === 'privacy' ? { href: '/terms', label: '用户协议' } : { href: '/privacy', label: '隐私政策' };

  return (
    <div className="legal-page">
      <div className="legal-page__wrap">
        <header className="legal-page__header">
          <div className="legal-page__brand">糊涂健康 / miyf</div>
          <h1>{doc.title}</h1>
          <div className="legal-page__meta">生效日期：{doc.updated} · 本页无需登录即可访问</div>
        </header>
        <article className="legal-page__article">{doc.body}</article>
        <footer className="legal-page__footer">
          <a href={other.href}>{other.label}</a>
          <a href="/login">返回登录</a>
        </footer>
      </div>
    </div>
  );
}
