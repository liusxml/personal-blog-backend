import type { ReactNode } from 'react';
import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';

type FeatureItem = {
  title: string;
  Svg: React.ComponentType<React.ComponentProps<'svg'>>;
  description: ReactNode;
  emoji?: string;
};

const FeatureList: FeatureItem[] = [
  {
    title: '🏗️ 模块化单体架构',
    emoji: '🏗️',
    Svg: require('@site/static/img/undraw_docusaurus_mountain.svg').default,
    description: (
      <>
        采用 <strong>API-Service 双层分离</strong>设计，为微服务演进预留空间。
        基于 <strong>Spring Boot 3.5.7 + Java 21</strong>，
        使用 MyBatis-Plus 3.5.14 增强 CRUD 能力。
      </>
    ),
  },
  {
    title: '⚡ 开箱即用的 Base 框架',
    emoji: '⚡',
    Svg: require('@site/static/img/undraw_docusaurus_tree.svg').default,
    description: (
      <>
        <strong>BaseServiceImpl</strong> 提供安全的增量更新、自动 DTO 验证和 MapStruct 集成。
        内置 <strong>JWT 认证</strong>、<strong>Redis 缓存</strong>、
        <strong>统一异常处理</strong>，让你专注于业务逻辑。
      </>
    ),
  },
  {
    title: '🧪 企业级测试与文档',
    emoji: '🧪',
    Svg: require('@site/static/img/undraw_docusaurus_react.svg').default,
    description: (
      <>
        完整的 <strong>ArchUnit 架构测试</strong>确保代码质量，
        覆盖单元测试、集成测试。配备详尽的 <strong>Docusaurus 文档</strong>，
        从快速上手到深入指南一应俱全。<span style={{ color: '#25c2a0' }}>项目评分 9.2/10</span> ⭐
      </>
    ),
  },
];

function Feature({ title, Svg, description, emoji }: FeatureItem) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        {emoji ? (
          <div style={{ fontSize: '4rem', marginBottom: '1rem' }}>{emoji}</div>
        ) : (
          <Svg className={styles.featureSvg} role="img" />
        )}
      </div>
      <div className="text--center padding-horiz--md">
        <Heading as="h3">{title}</Heading>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures(): ReactNode {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
